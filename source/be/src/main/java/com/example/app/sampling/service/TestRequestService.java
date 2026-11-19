package com.example.app.sampling.service;

import com.example.app.sampling.dto.TestRequestDtos.*;
import com.example.app.sampling.entity.*;
import com.example.app.sampling.repository.*;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class TestRequestService {
 private final TestRequestRepository requests; private final SamplingPlanRepository plans; private final FieldSamplingRepository fields;
 public TestRequestService(TestRequestRepository r,SamplingPlanRepository p,FieldSamplingRepository f){requests=r;plans=p;fields=f;}
 @Transactional(readOnly=true) public List<Response> list(){return requests.findAll(Sort.by(Sort.Direction.DESC,"id")).stream().limit(100).map(this::response).toList();}
 @Transactional(readOnly=true) public Response get(Long id){return response(require(id));}
 @Transactional(readOnly=true) public Response byNumber(String number){return response(requests.findByRequestNumberIgnoreCase(normalize(number)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy Phiếu yêu cầu")));}
 @Transactional public Response create(SaveRequest q,User actor){String number=normalize(q.requestNumber());if(requests.existsByRequestNumberIgnoreCase(number))throw new ResponseStatusException(HttpStatus.CONFLICT,"Số Phiếu yêu cầu đã tồn tại");TestRequest r=new TestRequest(number,actor);apply(r,q);return response(requests.save(r));}
 @Transactional public Response update(Long id,SaveRequest q,User actor){TestRequest r=require(id);requireAdminIfCompleted(r,actor);String n=normalize(q.requestNumber());if(!r.getRequestNumber().equalsIgnoreCase(n))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không đổi số Phiếu yêu cầu sau khi tạo");apply(r,q);return response(r);}
 @Transactional public Response savePlan(Long id,SavePlan q,User actor){TestRequest r=require(id);SamplingPlan p=plans.findByTestRequest_Id(id).orElseGet(()->new SamplingPlan(r));if(p.getId()==null&&plans.existsByFormNumberIgnoreCase(normalize(q.formNumber())))throw new ResponseStatusException(HttpStatus.CONFLICT,"Số Kế hoạch lấy mẫu đã tồn tại");p.update(normalize(q.formNumber()),q.formDate(),trim(q.representative()),q.sampleCollector().trim(),trim(q.collectorPhone()),q.samplingStart(),q.samplingEnd(),trim(q.samplingLocation()),trim(q.growingArea()),trim(q.packingFacility()),trim(q.vehicleNumber()),trim(q.trailerNumber()),trim(q.containerNumber()),trim(q.schemeNotes()),trim(q.transportConditions()));plans.save(p);return response(r);}
 @Transactional public Response saveField(Long id,SaveField q,User actor){TestRequest r=require(id);FieldSampling f=fields.findByTestRequest_Id(id).orElseGet(()->new FieldSampling(r));if(f.getId()==null&&fields.existsByFormNumberIgnoreCase(normalize(q.formNumber())))throw new ResponseStatusException(HttpStatus.CONFLICT,"Số Phiếu lấy mẫu đã tồn tại");f.update(normalize(q.formNumber()),q.formDate(),trim(q.representative()),q.sampleCollector().trim(),trim(q.collectorPhone()),q.samplingStart(),q.samplingEnd(),trim(q.samplingLocation()),trim(q.growingArea()),trim(q.packingFacility()),trim(q.vehicleNumber()),trim(q.trailerNumber()),trim(q.containerNumber()),trim(q.sampleCondition()),trim(q.storageCondition()),trim(q.sampleSealNumber()),q.sealPresent(),trim(q.samplingMethod()),trim(q.sampleQuantity()),trim(q.fieldNotes()));fields.save(f);return response(r);}
 private void apply(TestRequest r,SaveRequest q){r.update(q.requestDate(),q.customerName().trim(),trim(q.taxCode()),trim(q.contactPerson()),trim(q.customerAddress()),q.customerPhone().trim(),trim(q.customerEmail()),q.sampleName().trim(),trim(q.lotCode()),trim(q.variety()),trim(q.origin()),q.harvestDate(),q.packingDate(),q.samplingMode(),trim(q.purpose()),q.expectedDate(),trim(q.resultDeliveryMethod()),trim(q.serviceConditions()),trim(q.delayNotice()),q.completed());int[] order={0};r.replaceItems(q.items().stream().map(i->new TestRequestItem(++order[0],i.sampleNameOrId().trim(),i.parameters().trim(),trim(i.matrix()),trim(i.testMethod()))).toList());}
 private Response response(TestRequest r){return Response.from(r,plans.findByTestRequest_Id(r.getId()).orElse(null),fields.findByTestRequest_Id(r.getId()).orElse(null));}
 private TestRequest require(Long id){return requests.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy Phiếu yêu cầu"));}
 private void requireAdminIfCompleted(TestRequest r,User actor){if("COMPLETED".equals(r.getStatus())&&actor.getRole()!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ Admin được sửa Phiếu yêu cầu đã hoàn tất");}
 private static String normalize(String v){if(v==null||v.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số phiếu không được để trống");return v.trim().toUpperCase();} private static String trim(String v){return v==null||v.isBlank()?null:v.trim();}
}
