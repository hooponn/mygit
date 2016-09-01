import java.io.IOException;
import java.io.InputStream;

public class Test {
	public static void main(String[] args) {
		final FreeSocketClient freeSocketClient = FreeSocketClient.getInstance();
		freeSocketClient.setAdrress("192.168.1.106", 6060);//设置ip和端口
		freeSocketClient.setIsKeepAlive(true);//打开心跳开关
		freeSocketClient.setAliveDelayTime(3000);//心跳间隔时间
		freeSocketClient.setAliveMsg(new byte[] { 8 });//固定心跳消息
		freeSocketClient.connect();//连接
		freeSocketClient.setStartKeepAlive(true);//开始心跳
		new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					freeSocketClient.send("123".getBytes());//模拟发送消息
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
		/**
		 * 回调结果
		 */
		freeSocketClient.getResponse(new OnResponseFinish() {
			/**
			 * 心跳反馈，true代表有回应，否则没有。具体回应的消息对不对在下面接收消息里判断
			 */
			@Override
			public void setKeepAliveMsg(boolean isAlive) {
				System.out.println(isAlive);
				/**
				 * 动态设置心跳消息
				 */
				//freeSocketClient.setAliveMsg(new byte[] { 8 });
				
			}
			/**
			 * 返回一个InputStream，根据需求处理
			 */
			@Override
			public void getInputStream(InputStream in) {
				int n = 0;
				try {
					n = in.read();//测试接收消息
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(n);
			}
			/**
			 * 异常消息返回
			 */
			@Override
			public void clientException(String errMsg) {
				System.out.println(errMsg);//打印错误日志
			}
		});
	}

}
