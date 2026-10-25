package cn.workbench;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
class AttachmentSafetyTest {
 byte[] ole(String stream){byte[] bytes=new byte[512];byte[] magic={(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1};System.arraycopy(magic,0,bytes,0,magic.length);byte[] n=stream.getBytes(StandardCharsets.UTF_16LE);System.arraycopy(n,0,bytes,128,n.length);return bytes;}
 byte[] zip(String entry,byte[] content)throws IOException{var out=new ByteArrayOutputStream();try(var z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry(entry));z.write(content);z.closeEntry();}return out.toByteArray();}
 @Test void confirmedLegacyOfficeTypesRemainAvailable(){assertThat(AttachmentController.validate("report.doc",ole("WordDocument"),"application/msword")).isEqualTo("application/msword");assertThat(AttachmentController.validate("report.xls",ole("Workbook"),"application/vnd.ms-excel")).isEqualTo("application/vnd.ms-excel");}
 @Test void modernOfficeWhitelistRemainsPending(){assertThatThrownBy(()->AttachmentController.validate("report.xlsx",ole("Workbook"),"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).isInstanceOf(ApiException.class);}
 @Test void normalArchiveIsValidatedWithoutExtraction()throws Exception{assertThat(AttachmentController.validate("results.zip",zip("folder/result.txt","Synthetic result".getBytes(StandardCharsets.UTF_8)),"application/zip")).isEqualTo("application/zip");}
 @Test void archiveTraversalAndScriptsAreRejected()throws Exception{for(String name:new String[]{"../escape.txt","/absolute.txt","C:/escape.txt","folder/launch.exe","folder/script.js"}){byte[] bytes=zip(name,"Synthetic".getBytes(StandardCharsets.UTF_8));assertThatThrownBy(()->AttachmentController.validate("results.zip",bytes,"application/zip")).isInstanceOf(ApiException.class);}}
 @Test void archiveDecompressionBombIsRejected()throws Exception{byte[] bytes=zip("large.txt",new byte[2*1024*1024]);assertThatThrownBy(()->AttachmentController.validate("results.zip",bytes,"application/zip")).isInstanceOf(ApiException.class);}
}
