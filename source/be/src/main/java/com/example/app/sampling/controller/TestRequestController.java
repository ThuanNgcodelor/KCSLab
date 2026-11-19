package com.example.app.sampling.controller;
import com.example.app.sampling.dto.TestRequestDtos.*;
import com.example.app.sampling.service.TestRequestService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/test-requests")
public class TestRequestController {
 private final TestRequestService service; private final UserService users;
 public TestRequestController(TestRequestService s,UserService u){service=s;users=u;}
 @GetMapping public List<Response> list(){return service.list();}
 @GetMapping("/{id}") public Response get(@PathVariable Long id){return service.get(id);}
 @GetMapping("/by-number/{number}") public Response byNumber(@PathVariable String number){return service.byNumber(number);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Response create(@Valid @RequestBody SaveRequest q,@AuthenticationPrincipal Jwt jwt){return service.create(q,users.getByEmail(jwt.getSubject()));}
 @PutMapping("/{id}") public Response update(@PathVariable Long id,@Valid @RequestBody SaveRequest q,@AuthenticationPrincipal Jwt jwt){return service.update(id,q,users.getByEmail(jwt.getSubject()));}
 @PutMapping("/{id}/sampling-plan") public Response plan(@PathVariable Long id,@Valid @RequestBody SavePlan q,@AuthenticationPrincipal Jwt jwt){return service.savePlan(id,q,users.getByEmail(jwt.getSubject()));}
 @PutMapping("/{id}/field-sampling") public Response field(@PathVariable Long id,@Valid @RequestBody SaveField q,@AuthenticationPrincipal Jwt jwt){return service.saveField(id,q,users.getByEmail(jwt.getSubject()));}
}
