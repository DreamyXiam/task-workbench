package cn.workbench;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import static org.assertj.core.api.Assertions.*;
class ExportFormatsTest {
 @Test void xlsxStoresFormulaLikeValuesAsInlineTextAndEscapesXml() throws Exception {
  byte[] bytes=ExportFormats.xlsx("Actor 42 time 2026-10-24",List.of("Value"),List.of(List.of("=SUM(A1:A2)"),List.of("+cmd"),List.of("-1"),List.of("@x"),List.of("<tag>&\"")));
  Map<String,String> entries=new HashMap<>();try(var z=new ZipInputStream(new ByteArrayInputStream(bytes))){for(ZipEntry e;(e=z.getNextEntry())!=null;)entries.put(e.getName(),new String(z.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
  assertThat(entries).containsKeys("[Content_Types].xml","xl/workbook.xml","xl/worksheets/sheet1.xml");
  assertThat(entries.get("xl/worksheets/sheet1.xml")).contains("t=\"inlineStr\"","=SUM(A1:A2)","+cmd","@x","&lt;tag&gt;&amp;&quot;","Actor 42").doesNotContain("<f>");
 }
 @Test void pdfContainsSameSnapshotNumbersAndWatermarkOnEveryPage() throws Exception {
  var rows=new ArrayList<List<String>>();for(int i=0;i<100;i++)rows.add(List.of("Dept "+i,"85.83","N/A"));
  byte[] bytes=ExportFormats.pdf("Actor 42 time 2026-10-24",List.of("Department","Score","Adjusted"),rows,"");
  try(var doc=Loader.loadPDF(bytes)){assertThat(doc.getNumberOfPages()).isGreaterThan(1);String text=new PDFTextStripper().getText(doc);assertThat(text).contains("85.83","N/A","Dept 99");for(int i=1;i<=doc.getNumberOfPages();i++){var strip=new PDFTextStripper();strip.setStartPage(i);strip.setEndPage(i);assertThat(strip.getText(doc)).contains("Actor 42");}}
 }
 @Test void excessiveExportsFailExplicitly(){assertThatThrownBy(()->ExportFormats.xlsx("test",List.of("x"),Collections.nCopies(5001,List.of("x")))).isInstanceOf(ApiException.class);}
}
