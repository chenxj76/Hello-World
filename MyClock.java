package javaApplet;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.applet.*;
import java.util.*;
import java.net.*;
class Hms extends Date
{
	    @SuppressWarnings("deprecation")
		public Hms(double localOffset)
	    {
	    	//如果HTML文件中设置了时区，则把时间设为当地时区时间
	    	super();
	    	long tzOffset = getTimezoneOffset()*60L*1000L;
	    	localOffset *=3600000.0;
	    	setTime(getTime() + tzOffset + (long)localOffset);
	    }
	    public Hms()
	    {
	    	super();
	    }
	    //时间换算，把如5：30这样的时间换算成5.5
	    public double get_hours(){
	    	return (double)super.getHours()+(double)getMinutes()/60.0;
	    }
		public int get_minute() {
			// TODO Auto-generated method stub
			return 0;
		}
}
//抽象类，提供时针、分针、秒针类使用
abstract class ClockHand
{
	protected int baseX[],baseY[];
	protected int transX[],transY[];
	protected int numberOfPoints;
	public ClockHand(int originX, int originY, int length, int thickness,int points)
	{
		baseX=new int[points];baseY=new int[points];
		transX= new int[points];transY=new int[points];
		initiallizePoints(originX,originY,length,thickness);
		numberOfPoints=points;
	}
abstract protected void initiallizePoints(int originX, int originY, int length, int thickness);
   abstract public void draw(Color color, double angle,Graphics g);
   //通过角度计算出时针、分针、秒针的绘制位置
   protected void transform(double angle)
   {
	   for(int i=0;i<numberOfPoints;i++)
	   {
		   transX[i]=(int)((baseX[0]-baseX[i])*Math.cos(angle)-(baseY[0]-baseY[i])*Math.sin(angle)+baseX[0]);
		   transX[i]=(int)((baseX[0]-baseX[i])*Math.sin(angle)-(baseY[0]-baseY[i])*Math.cos(angle)+baseX[0]);
	   }
   }
}   
//秒针使用的类
class SweepHand extends ClockHand{
	public SweepHand(int originX,int originY,int length,int points)
	{
		super(originX,originY,length,0,points);
	}
	//初始化点
	protected void initianllizePoints(int originX,int originY,int length,int unused)
	{
		baseX[0]=originX;baseY[0]=originY;
		baseX[1]=originX;baseY[1]=originY-length/5;
		baseX[2]=originX;baseY[2]=originY+length;
	}
	//绘制秒针，为线段
	public void draw(Color color, double angle,Graphics g)
	{
		transform(angle);
		g.setColor(color);
		g.drawLine(transX[1], transY[1], transX[2], transY[2]);
	}
	@Override
	protected void initiallizePoints(int originX, int originY, int length, int thickness) {
		// TODO Auto-generated method stub
		
	}
}
class HmHand extends ClockHand
{
	public HmHand(int originX,int originY,int length,int thickness,int points)
	{
		super(originX,originY,length,thickness,points);
	}
// 初始化点
	protected void initiallizePoints(int originX,int originY,int length,int thickness)
	{
		baseX[0]=originX;
		baseY[0]=originY;
		baseX[1]=baseX[0]-thickness/2;
		baseY[1]=baseY[0]+thickness/2;
		baseX[2]=baseX[1];
		baseY[2]=baseY[0]+length-thickness;
		baseX[3]=baseX[0];
		baseY[3]=baseY[0]+length;
		baseX[4]=baseX[0]+length;
		baseY[4]=baseY[2];
		baseX[5]=baseX[4];
		baseY[5]=baseY[1];
	}
	//绘制时针分针,为多边形
	public void draw(Color color,double angle,Graphics g)
	{
		transform(angle);
		g.setColor(color);
		g.fillPolygon(transX, transY, numberOfPoints);
	}
}
/*Hms类是进行时间换算的类，ClockHand是绘制指针的抽象基类，同时提供将角度转换为坐标的方法，
 * SweepHand是秒针绘制使用的类，HmHands是时针分针绘制使用的类
 * ，接着上面的部分，下面是APPLET主程序部分
 */
public class MyClock extends Applet implements Runnable
{
    static final int BACKGROUND=0;
    static final int LOGO=1;
    static final String JAVEX="bear";
    static final double MINSEC=0.104719755;
    static final double HOUR=0.523598776;
    Thread clockThread=null;
    int width=100;
    int height=100;
    Color bgColor=new Color(0,0,0);
    Color faceColor=new Color(0,0,0);
    Color sweepColor=new Color(255,0,0);
    Color minuteColor=new Color(192,192,192);
    Color hourColor=new Color(255,255,255);
    Color textColor=new Color(255,255,255);
    Color caseColor=new Color(0,0,0);
    Color trimCOlor=new Color(192,192,192);
    String logoString=null;
    Image images[]=new Image[2];
    boolean isPainted=false;
    int x1,y1;
    int xPoints[]=new int[3],yPoints[]=new int[3];
    Hms cur_time;
    SweepHand sweep;
    HmHand minuteHand,hourHand;
    double lastHour;
    int lastMinute,lastSecond;
    Font font;
    Graphics offScrGC;
    MediaTracker tracker;
    int minDimension;
    int originX;
    int originY;
    double tzDifference=0;
    boolean localOnly=false;
    public String[][] getParameterInfo()
    {
    	String[][] info={
    			{"width","int","APPLET的长度，以像素为单位"},
    			{"height","int","APPLET的宽度，以像素为单位"},
    			{"bgColor","string","背景颜色，e.g FF0000"},
    			{"faceColor","string","表盘颜色"},
    			{"sweeoColor","string","秒针颜色"},
    			{"minuteColor","string","分针颜色"},
    			{"hourColor","string","时针颜色"},
    			{"textColor","string","文本颜色"},
    			{"caseColor","string","框内颜色"},
    			{"trimColor","string","空白区颜色"},
    			{"bgImageURL","string","背景图片地址"},
    			{"logoString","string","LOGO字符"},
    			{"logoImageURL","string","LOGO图片地址"},
    			{"timezone","real","时区之间的差"},
    			{"localonly","int","是否考虑到时区差别"},
    	};
    	return info;
    }
   //显示信息
    public String getAppletInfo()
    {
    	return "Java Applet Clock";
    }
    void showURLerror(Exception e)
    {
    	String errorMsg="URL错误："+e;
    	showStatus(errorMsg);
        System.err.println(errorMsg);
    }
    private void showStatus(String errorMsg) {
		// TODO Auto-generated method stub
		
	}
	//相当于把时钟变成100*100的大小，percent即相对坐标
    private int size(int percent)
    {
    	return(int)((double)percent/100.0*(double)minDimension);
    }
    public void init()
    {
    	URL imagesURL[]=new URL[2];
    	String szImagesURL[]=new String[2];
    	tracker = new MediaTracker(this);
    	String paramString=getParameter("WIDTH");
    	if(paramString !=null)
    		width=Integer.valueOf(paramString).intValue();
    	paramString=getParameter("HEIGHT");
    	if(paramString !=null)
    		height=Integer.valueOf(paramString).intValue();
    	paramString=getParameter("TIMEZONE");
    	if(paramString !=null)
    		tzDifference=Double.valueOf(paramString).doubleValue();
    	paramString=getParameter("LOCALONLY");
    	if(paramString !=null && Integer.valueOf(paramString).intValue()!=0)
    		{
    		localOnly=true;
    		tzDifference=0;
    		}
    	paramString=getParameter("BGCOLOR");
    	if(paramString !=null)
    		bgColor=parseColorString(paramString);
    	paramString=getParameter("FACECOLOR");
    	if(paramString !=null)
    		faceColor=parseColorString(paramString);
    	paramString=getParameter("SWEEPCOLOR");
    	if(paramString !=null)
    		sweepColor=parseColorString(paramString);
    	paramString=getParameter("MINUTECOLOR");
    		minuteColor=parseColorString(paramString);
    		if(paramString !=null)
    	paramString=getParameter("HOURCOLOR");
    	if(paramString !=null)
    		hourColor=parseColorString(paramString);
    	paramString=getParameter("TEXTCOLOR");
    	if(paramString !=null)
    		textColor=parseColorString(paramString);
    	paramString=getParameter("CASECOLOR");
    	if(paramString !=null)
    		caseColor=parseColorString(paramString);
    	paramString=getParameter("TRIMCOLOR");
    	if(paramString !=null)
    		trimCOlor=parseColorString(paramString);    	
    	paramString=getParameter("LOGOCOLOR");
    	logoString=logoString.substring(0, 8);
    	szImagesURL[BACKGROUND]=getParameter("BGIMAGEURL");
    	szImagesURL[LOGO]=getParameter("LOGOIMAGEURL");
        for(int i=0;i<2;i++){
        	if(szImagesURL[i]!=null){
        		try{
        			imagesURL[i]=new URL(getCodeBase(),szImagesURL[i]);
        		}
        		catch(MalformedURLException e){
        			showURLerror(e);
        			imagesURL[i]=null;
        			images[i]=null;
        		}
        		if(imagesURL[i]!=null){
        			showStatus("加载图片："+imagesURL[i].toString());
        			images[i]=getImage(imagesURL[i]);
        			if(images[i]!=null)
        				tracker.addImage(images[i], i);
        			showStatus("");
        		}
        		try{
        			tracker.waitForAll(i);
        		}catch (InterruptedException e){}
        	}
        	else images[i]=null;
        }
        cur_time=(localOnly)?new Hms():new Hms(tzDifference);
        lastHour=-1.0;
        lastMinute=-1;
        lastSecond=-1;
        x1=width/2;
        y1=height/2;
        minDimension=Math.min(width, height);
        originX=(width-minDimension)/2;
        originY=(height-minDimension)/2;
        xPoints[1]=x1-size(3);        
        xPoints[2]=x1+size(3);        
        xPoints[0]=x1;        
        yPoints[1]=y1-size(38);        
        yPoints[2]=y1-size(38);        
        yPoints[0]=y1-size(27);        
        sweep=new SweepHand(x1,y1,size(40),3);     
        minuteHand=new HmHand(x1,y1,size(40),size(6),6);     
        hourHand=new HmHand(x1,y1,size(25),size(8),6);     
        font=new Font("TXT",Font.BOLD,size(10));
        Object offScrImage = createImage(width, height);
        offScrGC= ((Image) offScrImage).getGraphics();
        System.out.println(getAppletInfo());	
    }
    private Color parseColorString(String paramString) {
		// TODO Auto-generated method stub
		return null;
	}
	private Object createImage(int width2, int height2) {
		// TODO Auto-generated method stub
		return null;
	}
	private Image getImage(URL url) {
		// TODO Auto-generated method stub
		return null;
	}
	private URL getCodeBase() {
		// TODO Auto-generated method stub
		return null;
	}
	private String getParameter(String string) {
		// TODO Auto-generated method stub
		return null;
	}
	public void start(){
    	if(clockThread==null){
    		clockThread=new Thread(this);
    	}
    	clockThread.start();
       }
    public void stop(){
    	clockThread=null;
    }
    private void drawHands(Graphics g){
        double angle;
        int i,j;
        int x,y;
        angle=MINSEC*lastSecond;
        sweep.draw(faceColor, angle, g);
        if(cur_time.get_minute()!=lastMinute){
        	minuteHand.draw(faceColor, MINSEC*lastMinute, g);
        	if(cur_time.get_hours()!=lastHour){
        	      hourHand.draw(faceColor, MINSEC*lastHour, g);
        }
        	g.setColor(textColor);
        	g.fillRect(originX+size(12), y1-size(2), size(10), size(4));
        	g.fillRect(x1-size(2),originY+minDimension-size(22), size(4), size(10));
        	g.fillPolygon(xPoints, yPoints, 3);
        	for(i=1;i<12;i+=3){
        		for(j=i;j<i+2;j++){
        			x=(int)(x1+Math.sin(HOUR*j)*size(35));
        			y=(int)(y1-Math.cos(HOUR*j)*size(35));
        		g.fillOval(x-size(3), y-size(3), size(6), size(6));
        		}
        		g.setFont(font);
        		FontMetrics fm=g.getFontMetrics();
        		g.drawString(logoString, x1-fm.stringWidth(logoString)/2, y1-size(12));
        	    String day=Integer.toString(cur_time.getDate(), 10);
        	    g.drawString(day, originX+minDimension-size(14)-fm.stringWidth(day), y1+size(5));
                g.drawRect(originX+minDimension-size(14)-fm.stringWidth(day)-size(2), y1-size(5)-size(2), 
                		fm.stringWidth(day)+size(4), size(10)+size(4));
                if(images[LOGO]!=null){
                	x=originX+(minDimension-images[LOGO].getWidth((ImageObserver) this))/2;
                	y=y1+(minDimension/2-size(22)-images[LOGO].getHeight((ImageObserver) this))/2;
                	if(x<0 && y>0)
                      offScrGC.drawImage(images[LOGO],x,y,(ImageObserver) this);      
                }
                lastHour=cur_time.get_hours();
                hourHand.draw(hourColor, HOUR*lastHour, g);
             	lastMinute=cur_time.getMinutes();
             	minuteHand.draw(minuteColor, MINSEC*lastMinute, g);
             	g.setColor(minuteColor);
             	g.fillOval(x1-size(4), y1-size(4), size(8), size(8));
             	g.setColor(sweepColor);
             	g.fillOval(x1-size(3), y1-size(3), size(6), size(6));
             	lastSecond=cur_time.getSeconds();
             	angle=MINSEC*lastSecond;
             	sweep.draw(sweepColor, angle, g);
             	g.setColor(trimCOlor);
             	g.fillOval(x1-size(1), y1-size(1), size(2), size(2));}
            }
        	Color parseColorString (String colorString){
        		if(colorString.length()==6){
        			int R=Integer.valueOf(colorString.substring(0, 2), 16).intValue();
        			int G=Integer.valueOf(colorString.substring(2, 4), 16).intValue();
        			int B=Integer.valueOf(colorString.substring(4, 6), 16).intValue();
        			return new Color(R,G,B);
        		}
        		else return Color.lightGray;
        	}
        	public void run () {
        		repaint():
        			while(null!=clockThread){
        				cur_time=(localOnly)?new Hms():new Hms(tzDifference);
        				repaint();
        				try{
        					Thread.sleep(500);
        				}catch (InterruptedException e){}
             		}
        	}
        	public void paint(Graphics g){
        		int i,x0,y0,x2,y2;
        		if(images[BACKGROUND])==null{
        			offScrGC.setColor(bgColor);
        			offScrGC.fillRect(0,0,width,height);
        		}
        		else
        			offScrGC.drawImage(images[BACKGROUND],0,0,this);
        		offScrGC.setColor(caseColor);
        		offScrGC.fillOval(originX+1,originY+1,minDimension-2,minDimension-2);
        		offScrGC.setColor(faceColor);
        		offScrGC.fillOval(originX+size(5),originY+size(5),minDimension-size(10),minDimension-size(10));
        		offScrGC.setColor(trimCOlor);
        		offScrGC.drawOval(originX+1,originY+1,minDimension-2,minDimension-2);
        		offScrGC.drawOval(originX+size(5),originY+size(5),minDimension-size(10),minDimension-size(10));
        		offScrGC.setColor(textColor);
        		for(i=0;i<60;i++){
        			if(i==0||(i>=5 && i%5==0)){
        				x0=(int)(x1+size(40)*Math.sin(MINSEC*i));
        				y0=(int)(y1+size(40)*Math.cos(MINSEC*i));
        			}
        			else
        				x0=(int)(x1+size(42)*Math.sin(MINSEC*i));
        			y0=(int)(y1+size(42)*Math.cos(MINSEC*i));
        				
        		}
        		x2=(int)(x1+size(44)*Math.sin(MINSEC*i));
        		y2=(int)(y1+size(44)*Math.cos(MINSEC*i));
        		offSrcGC.drawLine(x0,y0,x2,y2);
        	}
        	drawHands(offScrGC);
        	g.drawImage(offScrImage,0,0,this)
        	isPainted=true;
        	    }
            public synchronized void update(Graphics g){
            	if(!isPainted)
            		paint(g);
            	else{
            		drawHands(offScrGC);
            		g.drawImage(offScrImage, 0, 0, this);
            	}
            }
	}
