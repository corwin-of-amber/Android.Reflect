package amber.corwin.androidreflect.reflect;


public class ValueParser {

    public Object parseValue(String text, String type) throws ValueFormatError {
		if (type.equals("int"))
			return Integer.parseInt(text);
		else if (type.equals("java.lang.String"))
			return text;
		else
			throw new ValueFormatError();
	}
	
	@SuppressWarnings("serial")
	public static class ValueFormatError extends Exception { }

}
