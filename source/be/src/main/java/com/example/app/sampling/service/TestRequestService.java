package com.example.app.sampling.service;

import com.example.app.sampling.dto.TestRequestDtos.*;
import com.example.app.sampling.entity.*;
import com.example.app.sampling.repository.*;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.common.dto.PagedResponse;
import com.example.app.common.query.ListQuery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class TestRequestService {
 private final TestRequestRepository requests; private final SamplingPlanRepository plans; private final FieldSamplingRepository fields; private final TestRequestNumberSequence numbers;
 public TestRequestService(TestRequestRepository r,SamplingPlanRepository p,FieldSamplingRepository f,TestRequestNumberSequence n){requests=r;plans=p;fields=f;numbers=n;}
 @Transactional(readOnly=true) public String nextNumber(){return numbers.peek();}
 private static final Map<String,String> SORT_FIELDS=Map.of("createdAt","createdAt","requestDate","requestDate","requestNumber","requestNumber","customerName","customerName","sampleName","sampleName","expectedDate","expectedDate","status","status");
 /** Paged list: search, status/sampling-mode filter, request-date range and whitelisted sort all run in the database. */
 @Transactional(readOnly=true) public PagedResponse<Response> search(String q,String status,String samplingMode,LocalDate from,LocalDate to,int page,int size,String sort){
  ListQuery.requireOrderedRange(from,to);
  if(ListQuery.hasText(status)&&!status.matches("DRAFT|COMPLETED"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trạng thái không hợp lệ");
  if(ListQuery.hasText(samplingMode)&&!samplingMode.matches("COMPANY|CUSTOMER"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hình thức lấy mẫu không hợp lệ");
  var parsed=ListQuery.parseSort(sort,SORT_FIELDS,"createdAt,desc");
  Specification<TestRequest> spec=(root,query,cb)->cb.conjunction();
  if(ListQuery.hasText(status))spec=spec.and((root,query,cb)->cb.equal(root.get("status"),status));
  if(ListQuery.hasText(samplingMode))spec=spec.and((root,query,cb)->cb.equal(root.get("samplingMode"),samplingMode));
  if(from!=null)spec=spec.and((root,query,cb)->cb.greaterThanOrEqualTo(root.get("requestDate"),from));
  if(to!=null)spec=spec.and((root,query,cb)->cb.lessThanOrEqualTo(root.get("requestDate"),to));
  if(ListQuery.hasText(q)){String pattern=ListQuery.contains(q);
   spec=spec.and((root,query,cb)->cb.or(java.util.stream.Stream.of("requestNumber","customerName","sampleName","lotCode","contactPerson","customerPhone").map(f->(Predicate)cb.like(cb.lower(root.get(f)),pattern,'\\')).toArray(Predicate[]::new)));}
  return PagedResponse.from(requests.findAll(spec,ListQuery.page(page,size,parsed)).map(this::response),parsed.canonical());}
 @Transactional(readOnly=true) public Response get(Long id){return response(require(id));}
 @Transactional(readOnly=true) public Response byNumber(String number){return response(requests.findByRequestNumberIgnoreCase(normalize(number)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy Phiếu yêu cầu")));}
 @Transactional public Response create(SaveRequest q,User actor){String number=resolveNumber(q.requestNumber());TestRequest r=new TestRequest(number,actor);apply(r,q);return response(requests.save(r));}
 private String resolveNumber(String typed){
  if(typed==null||typed.isBlank())return numbers.allocate(numbers.currentYearCode());
  String number=TestRequestNumberSequence.normalize(typed);
  if(number==null||!TestRequestNumberSequence.FORMAT.matcher(number).matches())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số phiếu phải theo dạng CFC26.0001 (CFC, 2 số năm, dấu chấm, tối thiểu 4 số thứ tự)");
  numbers.register(number);
  if(requests.existsByRequestNumberIgnoreCase(number))throw new ResponseStatusException(HttpStatus.CONFLICT,"Số "+number+" đã tồn tại. Số kế tiếp là "+numbers.nextAfter(number));
  return number;}
 @Transactional public Response update(Long id,SaveRequest q,User actor){TestRequest r=require(id);requireAdminIfCompleted(r,actor);String n=q.requestNumber();if(n!=null&&!n.isBlank()&&!r.getRequestNumber().equalsIgnoreCase(normalize(n)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không đổi số Phiếu yêu cầu sau khi tạo");apply(r,q);return response(r);}
 @Transactional public Response savePlan(Long id,SavePlan q,User actor){TestRequest r=require(id);SamplingPlan p=plans.findByTestRequest_Id(id).orElseGet(()->new SamplingPlan(r));p.update(r.getRequestNumber(),q.formDate(),trim(q.representative()),q.sampleCollector().trim(),trim(q.collectorPhone()),q.samplingStart(),q.samplingEnd(),trim(q.samplingLocation()),trim(q.growingArea()),trim(q.packingFacility()),trim(q.vehicleNumber()),trim(q.trailerNumber()),trim(q.containerNumber()),trim(q.schemeNotes()),trim(q.transportConditions()));p.updateScheme(trim(q.representativePhone()),trim(q.sampleForms()),trim(q.lotSizeBand()),trim(q.transportModes()));plans.save(p);return response(r);}
 @Transactional public Response saveField(Long id,SaveField q,User actor){TestRequest r=require(id);FieldSampling f=fields.findByTestRequest_Id(id).orElseGet(()->new FieldSampling(r));f.update(r.getRequestNumber(),q.formDate(),trim(q.representative()),q.sampleCollector().trim(),trim(q.collectorPhone()),q.samplingStart(),q.samplingEnd(),trim(q.samplingLocation()),trim(q.growingArea()),trim(q.packingFacility()),trim(q.vehicleNumber()),trim(q.trailerNumber()),trim(q.containerNumber()),trim(q.sampleCondition()),trim(q.storageCondition()),trim(q.sampleSealNumber()),q.sealPresent(),trim(q.samplingMethod()),trim(q.sampleQuantity()),trim(q.fieldNotes()));f.updateExtras(trim(q.representativePhone()),trim(q.storageTemperature()));if(q.items()!=null){int[] order={0};f.replaceItems(q.items().stream().map(i->new FieldSamplingItem(++order[0],i.sampleName().trim(),trim(i.samplingMethod()),trim(i.quantity()),trim(i.parameters()),trim(i.parametersOther()))).toList());}fields.save(f);return response(r);}
 private void apply(TestRequest r,SaveRequest q){r.update(q.requestDate(),q.customerName().trim(),trim(q.taxCode()),trim(q.contactPerson()),trim(q.customerAddress()),q.customerPhone().trim(),trim(q.customerEmail()),q.sampleName().trim(),trim(q.lotCode()),trim(q.variety()),trim(q.origin()),q.harvestDate(),q.packingDate(),q.samplingMode(),trim(q.purpose()),q.expectedDate(),trim(q.resultDeliveryMethod()),trim(q.serviceConditions()),trim(q.delayNotice()),q.completed());int[] order={0};r.replaceItems(q.items().stream().map(i->new TestRequestItem(++order[0],i.sampleNameOrId()==null||i.sampleNameOrId().isBlank()?"Mẫu "+order[0]:i.sampleNameOrId().trim(),i.parameters().trim(),trim(i.matrix()),trim(i.testMethod()))).toList());}
 private Response response(TestRequest r){return Response.from(r,plans.findByTestRequest_Id(r.getId()).orElse(null),fields.findByTestRequest_Id(r.getId()).orElse(null));}
 private TestRequest require(Long id){return requests.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy Phiếu yêu cầu"));}
 private void requireAdminIfCompleted(TestRequest r,User actor){if("COMPLETED".equals(r.getStatus())&&actor.getRole()!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ Admin được sửa Phiếu yêu cầu đã hoàn tất");}
 private static String normalize(String v){if(v==null||v.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số phiếu không được để trống");String canonical=TestRequestNumberSequence.normalize(v);return canonical!=null?canonical:v.trim().toUpperCase();} private static String trim(String v){return v==null||v.isBlank()?null:v.trim();}
}
