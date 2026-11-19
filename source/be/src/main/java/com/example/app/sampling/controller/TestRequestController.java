package com.example.app.sampling.controller;
import com.example.app.sampling.dto.TestRequestDtos.*;
import com.example.app.sampling.service.TestRequestService;
import com.example.app.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.example.app.common.dto.PagedResponse;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/v1/test-requests")
public class TestRequestController {
 private final TestRequestService service; private final UserService users;
 public TestRequestController(TestRequestService s,UserService u){service=s;users=u;}
 @GetMapping public PagedResponse<Response> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt,desc") String sort,
   @RequestParam(required=false,name="q") String query,@RequestParam(required=false) String status,@RequestParam(required=false) String samplingMode,
   @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
  return service.search(query,status,samplingMode,from,to,page,size,sort);}
 @GetMapping("/next-number") public NextNumber nextNumber(){return new NextNumber(service.nextNumber());}
 @GetMapping("/{id}") public Response get(@PathVariable Long id){return service.get(id);}
 @GetMapping("/by-number/{number}") public Response byNumber(@PathVariable String number){return service.byNumber(number);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Response create(@Valid @RequestBody SaveRequest q,@AuthenticationPrincipal Jwt jwt){return service.create(q,users.getByEmail(jwt.getSubject()));}
 @PutMapping("/{id}") public Response update(@PathVariable Long id,@Valid @RequestBody SaveRequest q,@AuthenticationPrincipal Jwt jwt){return service.update(id,q,users.getByEmail(jwt.getSubject()));}
 @PutMapping("/{id}/sampling-plan") public Response plan(@PathVariable Long id,@Valid @RequestBody SavePlan q,@AuthenticationPrincipal Jwt jwt){return service.savePlan(id,q,users.getByEmail(jwt.getSubject()));}
 @PutMapping("/{id}/field-sampling") public Response field(@PathVariable Long id,@Valid @RequestBody SaveField q,@AuthenticationPrincipal Jwt jwt){return service.saveField(id,q,users.getByEmail(jwt.getSubject()));}
}
