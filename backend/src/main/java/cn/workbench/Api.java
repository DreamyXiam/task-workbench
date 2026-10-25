package cn.workbench;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import jakarta.servlet.http.*;import java.util.*;
@RestControllerAdvice public class Api {
 public static Map<String,Object> ok(Object data){Map<String,Object> r=new LinkedHashMap<>();r.put("code","OK");r.put("message","成功");r.put("data",data);r.put("traceId",trace());return r;}
 public static String trace(){var a=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();return a instanceof org.springframework.web.context.request.ServletRequestAttributes s?String.valueOf(s.getRequest().getAttribute("traceId")):UUID.randomUUID().toString();}
 public static Map<String,Object> error(String code,String message){return Map.of("code",code,"message",message,"traceId",trace());}
 @ExceptionHandler(ApiException.class) ResponseEntity<?> business(ApiException e){return ResponseEntity.status(e.status).body(error(e.code,e.getMessage()));}
 @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.bind.MethodArgumentNotValidException.class,IllegalArgumentException.class,org.springframework.web.multipart.MaxUploadSizeExceededException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(error("INVALID_REQUEST","请求格式或字段不合法"));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) ResponseEntity<?> conflict(Exception e){return ResponseEntity.status(409).body(error("CONFLICT","重复或冲突的数据"));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unknown(Exception e){java.util.logging.Logger.getLogger(Api.class.getName()).severe("Request failed trace="+trace()+" type="+e.getClass().getSimpleName());return ResponseEntity.status(500).body(error("INTERNAL_ERROR","请求处理失败，请联系管理员并提供追踪号"));}
}
