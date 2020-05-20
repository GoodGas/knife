package U2C;

import java.awt.Component;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import burp.BurpExtender;
import burp.Getter;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IMessageEditorController;
import burp.IMessageEditorTab;
import burp.IMessageEditorTabFactory;
import burp.IRequestInfo;
import burp.IResponseInfo;
import burp.ITextEditor;

public class U2CTab implements IMessageEditorTab,IMessageEditorTabFactory
{
	private ITextEditor txtInput;
	private byte[] originContent;
	private byte[] displayContent;
	private static IExtensionHelpers helpers;
	public U2CTab(IMessageEditorController controller, boolean editable, IExtensionHelpers helpers, IBurpExtenderCallbacks callbacks)
	{
		txtInput = callbacks.createTextEditor();
		txtInput.setEditable(editable);
		this.helpers = helpers;
	}

	@Override
	public String getTabCaption()
	{
		return "U2C";
	}

	@Override
	public Component getUiComponent()
	{
		return txtInput.getComponent();
	}

	@Override
	public boolean isEnabled(byte[] content, boolean isRequest)
	{
		try {
			if(content==null) {
				return false;
			}

			originContent = content;
			String contentStr = new String(content);

			//先尝试进行JSON格式的美化，如果其中有Unicode编码也会自动完成转换
			if (isJSON(content, isRequest)) {
				try {
					//Get only the JSON part of the content
					Getter getter = new Getter(helpers);
					byte[] body = getter.getBody(isRequest, content);
					List<String> headers = getter.getHeaderList(isRequest, content);

					displayContent = helpers.buildHttpMessage(headers, beauty(new String(body)).getBytes());
					//newContet = CharSet.covertCharSetToByte(newContet);
					return true;
				}catch(Exception e) {

				}
			}

			//如果不是JSON，或者JSON美化失败，就尝试Unicode转换
			int i=0;
			while (needtoconvert(contentStr) && i<=3) {
				//resp = Unicode.unicodeDecode(resp);//弃用
				contentStr = StringEscapeUtils.unescapeJava(contentStr);
				i++;
			}

			if (i>0) {//表明至少转换了一次了，需要显示
				displayContent = contentStr.getBytes();
				return true;
			}else {
				return false;
			}
		} catch (Exception e) {
			displayContent = e.getMessage().getBytes();
			e.printStackTrace(BurpExtender.getStderr());
			return false;
		}
	}

	@Override
	public void setMessage(byte[] content, boolean isRequest)
	{
		txtInput.setText(displayContent);
	}

	@Override
	public byte[] getMessage()
	{
		//byte[] text = txtInput.getText();
		//return text;
		return originContent;
		//change the return value of getMessage() method to the origin content to tell burp don't change the original response

	}

	@Override
	public boolean isModified()
	{
		//return txtInput.isTextModified();
		return false;
		//change the return value of isModified() method to false. to let burp don't change the original response) 
	}

	@Override
	public byte[] getSelectedData()
	{
		return txtInput.getSelectedText();
	}


	public static boolean isJSON(byte[] content,boolean isRequest) {
		if (isRequest) {
			IRequestInfo requestInfo = helpers.analyzeRequest(content);
			return requestInfo.getContentType() == IRequestInfo.CONTENT_TYPE_JSON;
		} else {
			IResponseInfo responseInfo = helpers.analyzeResponse(content);
			return responseInfo.getInferredMimeType().equals("JSON");
		}
	}

	public static String beauty(String inputJson) {
		//Take the input, determine request/response, parse as json, then print prettily.
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(inputJson);
		return gson.toJson(je);
	}

	public static boolean needtoconvert(String str) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		//Pattern pattern = Pattern.compile("(\\\\u([A-Fa-f0-9]{4}))");//和上面的效果一样
		Matcher matcher = pattern.matcher(str.toLowerCase());

		if (matcher.find() ){
			return true;
			//    		String found = matcher.group();
			//    		//！@#￥%……&*（）——-=，。；：“‘{}【】+
			//    		String chineseCharacter = "\\uff01\\u0040\\u0023\\uffe5\\u0025\\u2026\\u2026\\u0026\\u002a\\uff08\\uff09\\u2014\\u2014\\u002d\\u003d\\uff0c\\u3002\\uff1b\\uff1a\\u201c\\u2018\\u007b\\u007d\\u3010\\u3011\\u002b";
			//    		if (("\\u4e00").compareTo(found)<= 0 && found.compareTo("\\u9fa5")<=0)
			//    			return true;
			//    		else if(chineseCharacter.contains(found)){
			//    			return true;
			//    		}else{
			//    			return false;
			//    		}
		}else {
			return false;
		}
	}

	public static void main(String args[]) {
		System.out.print(needtoconvert("\\u0000"));
	}

	@Override
	public IMessageEditorTab createNewInstance(IMessageEditorController controller, boolean editable) {
		// TODO Auto-generated method stub
		return this;
	}
}