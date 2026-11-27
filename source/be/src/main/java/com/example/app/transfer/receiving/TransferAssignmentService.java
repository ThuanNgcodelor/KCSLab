package com.example.app.transfer.receiving;

import com.example.app.notification.entity.StaffNotificationType;
import com.example.app.notification.service.StaffNotificationService;
import com.example.app.transfer.entity.SampleTransfer;
import com.example.app.transfer.entity.TransferAssignee;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.receiving.ReceivingTransferResponse.Assignee;
import com.example.app.transfer.repository.SampleTransferRepository;
import com.example.app.transfer.repository.TransferAssigneeRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import com.example.app.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Tags technicians on a transfer so they know which samples they will test, and notifies the newly tagged ones. */
@Service
public class TransferAssignmentService {
    static final int MAX_ASSIGNEES = 20;
    private final TransferAssigneeRepository assignees;
    private final SampleTransferRepository transfers;
    private final UserRepository users;
    private final UserService userService;
    private final StaffNotificationService notifications;

    public TransferAssignmentService(TransferAssigneeRepository assignees, SampleTransferRepository transfers,
                                     UserRepository users, UserService userService, StaffNotificationService notifications) {
        this.assignees = assignees; this.transfers = transfers; this.users = users;
        this.userService = userService; this.notifications = notifications;
    }

    /** Active technicians that can be tagged. */
    @Transactional(readOnly = true)
    public List<Assignee> technicians() {
        return users.findByRoleInAndEnabledTrue(Set.of(Role.TECHNICIAN)).stream()
                .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
                .map(this::toAssignee).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Assignee>> assigneesOf(Collection<Long> transferIds) {
        Map<Long, List<Assignee>> result = new LinkedHashMap<>();
        if (transferIds.isEmpty()) return result;
        assignees.findByTransfer_IdInOrderByIdAsc(transferIds)
                .forEach(row -> result.computeIfAbsent(row.getTransfer().getId(), key -> new ArrayList<>()).add(toAssignee(row.getUser())));
        return result;
    }

    @Transactional(readOnly = true)
    public ReceivingTransferResponse attach(ReceivingTransferResponse response) {
        return response.withAssignees(assigneesOf(List.of(response.id())).getOrDefault(response.id(), List.of()));
    }

    @Transactional(readOnly = true)
    public List<ReceivingTransferResponse> attachAll(List<ReceivingTransferResponse> rows) {
        Map<Long, List<Assignee>> map = assigneesOf(rows.stream().map(ReceivingTransferResponse::id).toList());
        return rows.stream().map(row -> row.withAssignees(map.getOrDefault(row.id(), List.of()))).toList();
    }

    /** Replaces the tagged set. Only technicians can be tagged; only people newly added are notified. */
    @Transactional
    public List<Assignee> assign(Long transferId, Collection<Long> requestedIds, User actor) {
        if (actor.getRole() != Role.TECHNICIAN && actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ kỹ thuật viên hoặc Admin được gắn người thử nghiệm");
        }
        SampleTransfer transfer = transfers.findById(transferId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu chuyển mẫu"));
        if (transfer.getSubmittedAt() == null || (transfer.getStatus() != TransferStatus.PENDING && transfer.getStatus() != TransferStatus.ACCEPTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ gắn người thử nghiệm cho phiếu đang chờ nhận hoặc đã nhận");
        }
        Set<Long> wanted = new LinkedHashSet<>(requestedIds == null ? List.of() : requestedIds);
        if (wanted.size() > MAX_ASSIGNEES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tối đa " + MAX_ASSIGNEES + " người cho một phiếu");
        }
        Map<Long, User> chosen = new LinkedHashMap<>();
        for (User user : users.findAllById(wanted)) {
            if (user.getRole() != Role.TECHNICIAN || !user.isEnabled()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, user.getDisplayName() + " không phải kỹ thuật viên đang hoạt động");
            }
            chosen.put(user.getId(), user);
        }
        if (chosen.size() != wanted.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có người được chọn không tồn tại");
        }
        List<TransferAssignee> current = assignees.findByTransfer_IdOrderByIdAsc(transferId);
        Set<Long> existing = current.stream().map(row -> row.getUser().getId()).collect(Collectors.toSet());
        current.stream().filter(row -> !wanted.contains(row.getUser().getId())).forEach(assignees::delete);
        List<User> added = new ArrayList<>();
        for (User user : chosen.values()) {
            if (existing.contains(user.getId())) continue;
            assignees.save(new TransferAssignee(transfer, user, actor));
            added.add(user);
        }
        List<User> toNotify = added.stream().filter(user -> !user.getId().equals(actor.getId())).toList();
        if (!toNotify.isEmpty()) {
            String requestNumber = transfer.getReceipt().getRequestNumber();
            String reference = requestNumber == null || requestNumber.isBlank() ? "phiếu " + transfer.getTransferCode() : "Số/No. " + requestNumber;
            String names = toNotify.stream().map(User::getDisplayName).collect(Collectors.joining(", "));
            notifications.notifyUsers(toNotify, actor, StaffNotificationType.TRANSFER_ASSIGNED, "Được giao thử nghiệm mẫu",
                    "%s đã giao %s thử nghiệm %d mẫu của %s".formatted(actor.getDisplayName(), names, transfer.getItems().size(), reference),
                    "/admin/receiving/transfers/" + transferId, "TRANSFER", transferId);
        }
        return assigneesOf(List.of(transferId)).getOrDefault(transferId, List.of());
    }

    private Assignee toAssignee(User user) {
        return new Assignee(user.getId(), user.getDisplayName(), userService.avatarUrl(user));
    }
}
