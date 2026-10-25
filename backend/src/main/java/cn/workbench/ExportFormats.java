package cn.workbench;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

/** Bounded exports from one authorized, already materialized snapshot. */
final class ExportFormats {
 static final int MAX_ROWS=5000;
 static void validate(List<List<String>> rows){if(rows.size()>MAX_ROWS)throw new ApiException(413,"EXPORT_TOO_LARGE","导出最多5000行，请缩小查询范围");}
 static String clean(String s){return s==null?"":s.codePoints().filter(c->c==9||c==10||c==13||c>=32&&c!=0xFFFE&&c!=0xFFFF).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();}
 static String xml(String s){return clean(s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
 static byte[] xlsx(String watermark,List<String> headers,List<List<String>> rows){
  validate(rows);
  try(var output=new ByteArrayOutputStream();var zip=new ZipOutputStream(output,StandardCharsets.UTF_8)){
   put(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
   put(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
   put(zip,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
   put(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
   StringBuilder sheet=new StringBuilder("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"2\" topLeftCell=\"A3\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"20\" width=\"28\" customWidth=\"1\"/></cols><sheetData>");
   List<List<String>> all=new ArrayList<>();all.add(List.of(watermark));all.add(headers);all.addAll(rows);int row=0;
   for(var cells:all){sheet.append("<row r=\"").append(++row).append("\">");for(String cell:cells){if(cell!=null&&cell.length()>32767)throw new ApiException(413,"EXPORT_CELL_TOO_LONG","单元格文本过长，请缩小导出内容");sheet.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(cell)).append("</t></is></c>");}sheet.append("</row>");}
   sheet.append("</sheetData></worksheet>");put(zip,"xl/worksheets/sheet1.xml",sheet.toString());zip.finish();return output.toByteArray();
  }catch(IOException e){throw new ApiException(503,"EXPORT_UNAVAILABLE","暂时无法生成导出");}
 }
 static void put(ZipOutputStream z,String name,String body)throws IOException{z.putNextEntry(new ZipEntry(name));z.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"+body).getBytes(StandardCharsets.UTF_8));z.closeEntry();}
 static byte[] pdf(String watermark,List<String> headers,List<List<String>> rows,String fontPath){
  validate(rows);
  try(var document=new PDDocument();var output=new ByteArrayOutputStream()){
   PDFont font=fontPath==null||fontPath.isBlank()?new PDType1Font(FontName.HELVETICA):PDType0Font.load(document,Path.of(fontPath).toFile());
   List<String> lines=new ArrayList<>();lines.add(String.join(" | ",headers));for(var row:rows){lines.add(" ");for(int i=0;i<row.size();i++)lines.addAll(wrap(font,headers.get(Math.min(i,headers.size()-1))+": "+clean(row.get(i)).replace('\n',' ').replace('\r',' '),9,750));}
   int perPage=42,pages=Math.max(1,(lines.size()+perPage-1)/perPage);if(pages>200)throw new ApiException(413,"EXPORT_TOO_LARGE","PDF最多200页，请缩小查询范围");
   for(int page=0;page<pages;page++){var pdPage=new PDPage(new PDRectangle(PDRectangle.A4.getHeight(),PDRectangle.A4.getWidth()));document.addPage(pdPage);try(var stream=new PDPageContentStream(document,pdPage)){stream.beginText();stream.setFont(font,8);stream.newLineAtOffset(36,565);stream.showText(watermark);stream.newLineAtOffset(0,-18);stream.showText("Page "+(page+1)+" / "+pages);stream.setFont(font,9);for(int i=page*perPage;i<Math.min((page+1)*perPage,lines.size());i++){stream.newLineAtOffset(0,-12);stream.showText(lines.get(i));}stream.endText();}}
   document.getDocumentInformation().setTitle("Task Workbench snapshot");document.save(output);return output.toByteArray();
  }catch(IOException|IllegalArgumentException e){throw new ApiException(503,"PDF_FONT_UNAVAILABLE","PDF字体不可用，请管理员配置含中文的TASK_EXPORT_FONT（TTF）");}
 }
 static List<String> wrap(PDFont font,String s,float size,float width)throws IOException{
  var result=new ArrayList<String>();StringBuilder line=new StringBuilder();float current=0;
  for(int cp:s.codePoints().toArray()){String c=new String(Character.toChars(cp));float w=font.getStringWidth(c)/1000*size;if(current+w>width&&!line.isEmpty()){result.add(line.toString());line.setLength(0);current=0;}line.append(c);current+=w;}result.add(line.toString());return result;
 }
}
