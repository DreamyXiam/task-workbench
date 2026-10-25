package cn.workbench;
import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import org.springframework.http.*;import org.springframework.core.io.FileSystemResource;import org.springframework.beans.factory.annotation.Value;import jakarta.servlet.http.HttpServletRequest;import java.util.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.concurrent.TimeUnit;
@RestController @RequestMapping("/api/v1/attachments") public class AttachmentController {
 final Records r;final Policy p;final Writes w;final Path root;final String scanner;final Store db;
 public AttachmentController(Records r,Policy p,Writes w,Store d,@Value("${task.storage-root}")String root,@Value("${task.scanner-command:}")String scanner){this.r=r;this.p=p;this.w=w;db=d;this.root=Path.of(root).toAbsolutePath().normalize();this.scanner=scanner;}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Object upload(@RequestPart MultipartFile file,@RequestParam long taskId,@RequestParam(required=false)Long subTaskId,HttpServletRequest q)throws Exception{var a=p.actor(q);var task=p.task(a,taskId);if(Set.of("COMPLETED","CANCELLED").contains(task.get("status")))throw ApiException.conflict();if(subTaskId!=null){var sub=r.get(subTaskId);p.subtask(a,sub,true);if(Jsons.number(sub.get("taskId"))!=taskId)throw ApiException.denied();}String name=file.getOriginalFilename();if(name==null||name.length()>200||name.contains("/")||name.contains("\\")||name.contains("..")||name.chars().anyMatch(c->c<32))throw TaskService.invalid("文件名不合法");if(file.isEmpty()||file.getSize()>10*1024*1024)throw TaskService.invalid("文件大小须介于1字节至10MB");byte[] bytes=file.getBytes();String mime=validate(name,bytes,file.getContentType());String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));var body=Records.map("taskId",taskId,"subTaskId",subTaskId,"name",name,"sha256",sha,"size",bytes.length);return w.run(q,body,()->{try{Files.createDirectories(root);String key=UUID.randomUUID().toString()+".bin";Path target=root.resolve(key).normalize();if(!target.getParent().equals(root))throw ApiException.denied();Files.write(target,bytes,StandardOpenOption.CREATE_NEW);String scan=scan(target);var attachment=r.create("ATTACHMENT",a.company()==null?Jsons.integer(task.get("companyId")):a.company(),a.dept(),taskId,a.uid(),scan,Records.map("subTaskId",subTaskId,"uploaderId",a.uid(),"storageKey",key,"originalName",name,"mimeType",mime,"sizeBytes",bytes.length,"sha256",sha,"scanStatus",scan));w.audit(a,"ATTACHMENT_UPLOAD_"+scan,Jsons.number(attachment.get("id")));return publicView(attachment);}catch(java.io.IOException e){throw new ApiException(503,"STORAGE_UNAVAILABLE","文件存储暂不可用");}});}
 static String validate(String name,byte[] bytes,String supplied){
  String extension=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
  String mime=switch(extension){case "txt"->"text/plain";case "pdf"->"application/pdf";case "png"->"image/png";case "jpg","jpeg"->"image/jpeg";case "doc"->"application/msword";case "xls"->"application/vnd.ms-excel";case "zip"->"application/zip";default->throw TaskService.invalid("附件类型未获批准；现代Office格式等待白名单确认");};
  boolean valid=switch(extension){
   case "pdf"->bytes.length>5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-");
   case "png"->bytes.length>8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
   case "jpg","jpeg"->bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255;
   case "txt"->validText(bytes);
   case "doc"->legacyOle(bytes,"WordDocument");
   case "xls"->legacyOle(bytes,"Workbook")||legacyOle(bytes,"Book");
   case "zip"->{validateArchive(bytes);yield true;}
   default->false;
  };
  if(!valid||supplied==null||!mime.equals(supplied))throw TaskService.invalid("文件内容、扩展名与MIME不一致");
  return mime;
 }
 static boolean validText(byte[] bytes){try{String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();return !text.contains("\u0000")&&!text.stripLeading().startsWith("<");}catch(java.nio.charset.CharacterCodingException e){return false;}}
 static boolean legacyOle(byte[] bytes,String stream){
  byte[] signature={(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1};
  if(bytes.length<512||!Arrays.equals(Arrays.copyOf(bytes,8),signature))return false;
  byte[] expected=stream.getBytes(StandardCharsets.UTF_16LE);
  outer:for(int i=8;i+expected.length<=bytes.length;i++){for(int j=0;j<expected.length;j++)if(bytes[i+j]!=expected[j])continue outer;return true;}return false;
 }
 static void validateArchive(byte[] bytes){
  if(bytes.length<4||bytes[0]!='P'||bytes[1]!='K')throw TaskService.invalid("ZIP签名无效");
  long expanded=0;int entries=0;Set<String> names=new HashSet<>();
  try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))){
   java.util.zip.ZipEntry entry;byte[] buffer=new byte[8192];
   while((entry=zip.getNextEntry())!=null){
    String name=entry.getName().replace('\\','/');
    if(++entries>1000||name.length()>240||name.startsWith("/")||name.matches("^[A-Za-z]:.*")||name.indexOf(0)>=0||Arrays.asList(name.split("/")).contains("..")||!names.add(name))throw TaskService.invalid("ZIP路径或条目不安全");
    if(!entry.isDirectory()){
     String extension=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
     if(!Set.of("pdf","doc","xls","jpg","jpeg","png","txt").contains(extension))throw TaskService.invalid("ZIP含未批准的文件类型或嵌套压缩包");
    }
    long size=0;int n;while((n=zip.read(buffer))!=-1){size+=n;expanded+=n;if(expanded>100L*1024*1024)throw TaskService.invalid("ZIP展开大小超出安全限制");}
    long compressed=entry.getCompressedSize();
    if(size>=1024*1024&&compressed>0&&size>compressed*100)throw TaskService.invalid("ZIP压缩率超出安全限制");
    zip.closeEntry();
   }
   if(entries==0&&!(bytes[2]==5&&bytes[3]==6))throw TaskService.invalid("ZIP缺少有效条目");
  }catch(java.io.IOException e){throw TaskService.invalid("ZIP损坏、加密或无法安全读取");}
 }
 String scan(Path file){if(scanner.isBlank())return "QUARANTINED";try{Process process=new ProcessBuilder(scanner,file.toString()).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();return "QUARANTINED";}return switch(process.exitValue()){case 0->"CLEAN";case 1->"INFECTED";default->"QUARANTINED";};}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();return "QUARANTINED";}}
 Map<String,Object> authorized(Policy.Actor a,long id){var file=r.get(id);if(!"ATTACHMENT".equals(file.get("kind")))throw ApiException.denied();var task=p.task(a,Jsons.number(file.get("taskId")));if(!p.canSeeAttachment(a,file,task))throw ApiException.denied();return file;}
 @GetMapping("/{id}/{operation:download|preview}") ResponseEntity<?> download(@PathVariable long id,@PathVariable String operation,HttpServletRequest q){var a=p.actor(q);var attachment=authorized(a,id);if(!attachment.get("scanStatus").equals("CLEAN"))throw new ApiException(409,"FILE_QUARANTINED","文件尚未通过安全扫描");Path path=root.resolve(String.valueOf(attachment.get("storageKey"))).normalize();if(!path.getParent().equals(root)||!Files.isRegularFile(path))throw new ApiException(404,"NOT_FOUND","文件不存在");boolean preview=operation.equals("preview");if(preview&&!Set.of("application/pdf","image/png","image/jpeg").contains(attachment.get("mimeType")))throw new ApiException(422,"PREVIEW_UNSUPPORTED","此格式请下载后查看");w.audit(a,"ATTACHMENT_"+operation.toUpperCase(),id);return ResponseEntity.ok().header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").header("Cache-Control","no-store").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.builder(preview?"inline":"attachment").filename(String.valueOf(attachment.get("originalName")),StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(preview?String.valueOf(attachment.get("mimeType")):"application/octet-stream")).body(new FileSystemResource(path));}
 @GetMapping("/{id}/versions") Object versions(@PathVariable long id,HttpServletRequest q){var a=p.actor(q);var file=authorized(a,id);return Api.ok(r.children(Jsons.number(file.get("taskId")),"ATTACHMENT").stream().filter(x->Objects.equals(x.get("originalName"),file.get("originalName"))&&Objects.equals(x.get("subTaskId"),file.get("subTaskId"))&&Objects.equals(x.get("companyId"),file.get("companyId"))&&Objects.equals(x.get("deptId"),file.get("deptId"))&&p.canSeeAttachment(a,x,r.get(Jsons.number(file.get("taskId"))))).map(AttachmentController::publicView).toList());}
 static Map<String,Object> publicView(Map<String,Object>a){var copy=new LinkedHashMap<>(a);copy.remove("storageKey");List<String> actions=new ArrayList<>(List.of("versions"));if("CLEAN".equals(a.get("scanStatus"))){actions.add("download");if(Set.of("application/pdf","image/png","image/jpeg").contains(a.get("mimeType")))actions.add("preview");}copy.put("allowedActions",actions);return copy;}
}
