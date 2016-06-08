package com.example.testdemo.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.example.testdemo.util.DiskLruCache.Editor;
import com.example.testdemo.util.DiskLruCache.Snapshot;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

public class UidForImageLoader {
    protected static final int LOAD_FINISH = 101;
    protected static final int NEW_TASK = 102;
	public static int Thread_Count;//线程池中的数量，和手机CPU核心相关.
    public static Context c;//上下文
    public static LruCache<String, Bitmap> MemCache;//内存缓存
    public static LinkedBlockingDeque<Runnable> taskQueue;//双向队列   ，按需求选择LIFO还是FIFO
    public static ExecutorService exec;//线程池
    public static Handler pollhandler;//取下载任务的handler
    public static Handler uiHandler;//更新UI的handler
    public static Thread pollThread;//养活 pollHandler;
    public static DiskLruCache DiskCache;//磁盘缓存
    public static boolean isFirstTime=true;//确保只初始化一次
    /*初始化所有属性的值*/
    public static void init(Context context){
    	if(!isFirstTime){
			return;
		}
    	c=context;
    	Thread_Count=getNumberOfCores();
    	MemCache=new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory()/8)){
    		         @Override
    	             protected int sizeOf(String key, Bitmap value) {
						return value.getHeight()*value.getRowBytes();
    	             }
    	};
    	//初始化磁盘缓存
    			try {
    				DiskCache = DiskLruCache.open(directory(), appVersion(),1,1024*1024*10);
    			} catch (IOException e1) {
    				e1.printStackTrace();
    			}
    	taskQueue=new LinkedBlockingDeque<Runnable>();
    	exec=Executors.newFixedThreadPool(Thread_Count);
    	uiHandler=new Handler(Looper.getMainLooper()){
    		@Override
    		public void handleMessage(Message msg) {
    			if(msg.what==LOAD_FINISH){
    				ValueObject vo=(ValueObject) msg.obj;
    				ImageView iv=vo.iv;
    				String url=vo.url;
    				Bitmap bitmap=vo.bitmap;
    				if(iv.getTag().toString().equals(url)){
    					iv.setImageBitmap(bitmap);
    				}
    			}else{
    				super.handleMessage(msg);
    			}
    		}
    	};
    	pollThread=new Thread(){
    		@Override
			public void run() {
    			   Looper.prepare();
    	       pollhandler=new Handler(){
				@Override
    			public void handleMessage(Message msg) {
    				if(msg.what==NEW_TASK){
    					try {
							Runnable task=taskQueue.takeFirst();
							exec.execute(task);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
    				}else{
    			          	super.handleMessage(msg);
    				}
    				
    			}
    		};
    		Looper.loop();
    		}
    	};
    	pollThread.start();
    	isFirstTime=false;
    }
private static int appVersion() {
		
		try {
			PackageInfo info = c.getPackageManager().
			getPackageInfo(c.getPackageName(),0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}
private static File directory() {
		//有些环境在执行getExternalCacheDir获取
		//外部的缓存路径的时候可能会报空指针异常
		//所以这里采用了内部缓存路径----getCacheDir()
		String path = c.getExternalCacheDir().getPath();
		if(path==null){
			path=c.getCacheDir().getPath();
		}
		return new File(path,"imageloadercache");
	}
public static void loadImage(final String uid,final ImageView iv){
	   if(isFirstTime){
		   throw new RuntimeException("ImageLoad未做初始化");
	   }
	   final String md5Url=getMD5(uid);
	   iv.setTag(md5Url);
	   Bitmap bitmap=MemCache.get(md5Url);
	   if(bitmap!=null){
		//   Log.i("TAG","图片是从缓存中加载的");
		   iv.setImageBitmap(bitmap);
		   return;
	   }
	   try {
			//从磁盘缓存中试图取出url所对应的图片
			Snapshot snap = DiskCache.get(md5Url);
			if(snap!=null){
				Log.d("TAG","图像是从磁盘缓存中获取的");
				InputStream in = snap.getInputStream(0);
				bitmap = BitmapFactory.decodeStream(in);
				//将从磁盘缓存中获得的图片放到内存缓存存储一份
				MemCache.put(md5Url, bitmap);
				iv.setImageBitmap(bitmap);
				return;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	   taskQueue.add(new Runnable() {
		@Override
		public void run() {
	//发起网络连接，获得图片资源
		////////////////////根据uid解析出url/////////////////////////////////////////////	
			String imagePageUrl="http://map.baidu.com/detail?qt=ninf&uid="
					+uid+"&detail=life";
			try {
				Document doc = Jsoup.connect(imagePageUrl).timeout(2000).post();
				Elements elements=doc.select(".meta");
				String imageUrl = elements.get(0).getElementsByTag("a").
					first().getElementsByTag("img").attr("src");
	////////////////////////////////////////////////////////////////////////				
				URL u=new URL(imageUrl);
				HttpURLConnection connection=(HttpURLConnection) u.openConnection();
				connection.setRequestMethod("GET");
				connection.setDoInput(true);
				connection.connect();
				InputStream is=connection.getInputStream();
				//要对图片进行压缩
				Bitmap bitmap=compress(is,iv);
				is.close();
				MemCache.put(md5Url,bitmap);
				//将压缩后的图像放到磁盘缓存中存储
				Editor editor = DiskCache.edit(md5Url);
				OutputStream os = editor.newOutputStream(0);
				bitmap.compress(CompressFormat.JPEG, 100, os);
				editor.commit();
				//写日志(可选操作)
				DiskCache.flush();
				ValueObject value=new ValueObject(iv, md5Url, bitmap);
				Message.obtain(uiHandler,LOAD_FINISH,value).sendToTarget();
			} catch (Exception e) {
				e.printStackTrace();
			}	
			}
	});
	   Message.obtain(pollhandler,NEW_TASK).sendToTarget();
   } 	
   
protected static Bitmap compress(InputStream is, ImageView iv) {
//根据ImageView的尺寸大小进行压缩
	Bitmap bitmap=null;
	try {
		//1、先获得原始图像的尺寸大小
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		byte[] buff=new byte[4096];
		int len=-1;
		while((len=is.read(buff))!=-1){
			out.write(buff,0,len);
		}
		Options opts=new Options();
		opts.inJustDecodeBounds=true;
		BitmapFactory.decodeByteArray(out.toByteArray(),0,out.toByteArray().length, opts);
		int width=opts.outWidth;//图像的宽度
		int height=opts.outHeight;//图像的高度
		//2、获得希望的高和宽
		int targetWidth=iv.getWidth();
		int targetHeight=iv.getHeight();
		if(targetHeight==0||targetWidth==0){//如果ImageView的尺寸取值取不到
			targetHeight=c.getResources().getDisplayMetrics().heightPixels;//以当前设备屏幕高度赋值
			targetWidth=c.getResources().getDisplayMetrics().widthPixels;//以当前设备屏幕宽度赋值
		}
		//3、计算压缩比
		int sampleSize=1;
		if(width*1.0/targetWidth>1||height*1.0/targetHeight>1){
		sampleSize=(int)Math.ceil(Math.max(width*1.0/targetWidth,height*1.0/targetHeight));
					//ceil---向上取整
		}
		//4。压缩图片
		opts.inSampleSize=sampleSize;
		opts.inJustDecodeBounds=false;
		bitmap=BitmapFactory.decodeByteArray(out.toByteArray(),0,out.toByteArray().length, opts);
		out.close();
	} catch (Exception e) {
		e.printStackTrace();
	}
	return bitmap;
}
private static String getMD5(String url) {
	String result="";
	try {
		MessageDigest md=MessageDigest.getInstance("md5");
		md.update(url.getBytes());
		byte[] bytes=md.digest();
		StringBuilder sb=new StringBuilder();
		for(byte b:bytes){
			String str=Integer.toHexString(b&0xFF);
			if(str.length()==1){
				sb.append("0");
			}
			sb.append(str);
		}
		result=sb.toString();
	} catch (NoSuchAlgorithmException e) {
		e.printStackTrace();
	}
	return result;
}

private static int getNumberOfCores() {
		    File file=new File("/sys/devices/system/cpu/");
		 File[] files= file.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if(filename.matches("cpu[0-9]") ){
						return true;
					}else{
						return false;
					}
				}
			});
		if(files.length>0){
			return files.length;
		}else{
			return 1;
		}
	}
private static class ValueObject{
	ImageView iv;
	String url;
	Bitmap bitmap;
	public ValueObject(ImageView iv, String url, Bitmap bitmap) {
		super();
		this.iv = iv;
		this.url = url;
		this.bitmap = bitmap;
	}
	
}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
