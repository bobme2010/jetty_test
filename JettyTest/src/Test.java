import java.text.SimpleDateFormat;
import java.util.Date;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		long val = 1396408803964L ;
		//java.util.Date dt = new Date(val); 
		
		java.util.Date dt = new Date(val); 
		SimpleDateFormat sdf= new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		String sDateTime = sdf.format(dt);  //得到精确到秒的表示：08/31/2006 21:08:00
		System.out.println(sDateTime);
		//System.out.println(DateParser.parseDate(val));
		
	}

}
