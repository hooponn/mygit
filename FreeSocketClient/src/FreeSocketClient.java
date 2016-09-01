

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class FreeSocketClient {
	private int port;//  端口
	private String host;// 地址
	private Socket socket; // 套接字
	private InputStream inputStream; // 输入流
	private OutputStream outputStream; // 输出流
	// ----------------------------------------------//
	private boolean isCoreRun;// 核心线程开关
	private boolean isKeepAlive;// 是否打开心跳
	private boolean startKeepAlive;// 是否开始心跳
	private boolean hasKeepAliveMsg;//是否有心跳消息返回
	private int aliveDelayTime;// 心跳间隔时间
	private byte[] aliveMsg;// 心跳报文
	private OnResponseFinish OnResponseFinish;// 回调接口
	// ----------------------------------------------//
	private keepAliveThread aliveThread;// 心跳线程
	private CoreThread coreThread;// 核心线程(负责消息的发送和接收)
	// ----------------------------------------------//
	private LinkedBlockingQueue<byte[]> msgQueue = new LinkedBlockingQueue<byte[]>();// 消息队列初始化
	private static FreeSocketClient client = new FreeSocketClient();// 初始化

	// ----------------------------------------------//

	// 构造方法私有化
	private FreeSocketClient() {
	}

	/**
	 * 饿汉单例模式
	 */
	public static FreeSocketClient getInstance() {
		return client;
	}

	/**
	 * 设置地址和端口
	 * 
	 * @param host
	 *            地址 (String)
	 * @param port
	 *            端口 (int)
	 */
	public void setAdrress(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Socket 连接
	 */
	public void connect() {
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);// 延迟200ms，避免断线后马上重连
				} catch (InterruptedException e) {
					OnResponseFinish.clientException("连接线程睡眠时被意外中断");
				}
				try {
					socket = new Socket(host, port);// 连接
					inputStream = socket.getInputStream();// 获得输入流
					outputStream = socket.getOutputStream();// 获得输出流
				} catch (UnknownHostException e) {
					OnResponseFinish
							.clientException("UnknownHostException异常(指定的主机名或IP地址无法识别)");
				} catch (IOException e) {
					OnResponseFinish
							.clientException("连接Socket时发生IOException异常");
				}
				if (coreThread == null) {
					coreThread = new CoreThread(OnResponseFinish);
					new Thread(coreThread).start();// 启动核心线程
				}
				isCoreRun = true;
				if (isKeepAlive) {
					if (aliveThread == null) {
						aliveThread = new keepAliveThread();
						new Thread(aliveThread).start();// 启动心跳线程
					}
				}
			}
		}).start();
	}

	/**
	 * 断开Socket连接
	 */
	public void disConnect() {
		try {
			if (socket != null) {
				if (!socket.isInputShutdown()) {
					socket.shutdownInput();
				}
				if (!socket.isOutputShutdown()) {
					socket.shutdownOutput();
				}
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
				socket.close();
			}
		} catch (IOException e) {
			 OnResponseFinish.clientException("释放资源时出现异常");
		} finally {
			outputStream = null;
			inputStream = null;
			socket = null;
		}
	}

	/**
	 * 发送请求
	 * 
	 * @param req
	 *            请求 (byte[])
	 */
	public void send(byte[] req) {
		try {
			msgQueue.put(req);// 加入消息队列
		} catch (InterruptedException e) {
			OnResponseFinish.clientException("消息队列加入数据时被意外中断");
		}
	}

	/**
	 * 关闭所有，释放资源
	 */
	public void closeAll() {
		isCoreRun = false;
		isKeepAlive = false;
		disConnect();
	}

	/**
	 * 设置是否打开心跳
	 * 
	 * @param isKeepAlive
	 */
	public void setIsKeepAlive(boolean isKeepAlive) {
		this.isKeepAlive = isKeepAlive;
	}

	/**
	 * 设置是否开始心跳
	 * 
	 * @param startKeepAlive
	 */
	public void setStartKeepAlive(boolean startKeepAlive) {
		this.startKeepAlive = startKeepAlive;
	}

	/**
	 * 是否已经连接
	 * 
	 * @return true 连接状态 , false 未连接状态
	 */
	public boolean isConnected() {
		return socket.isConnected();
	}

	/**
	 * 设置心跳报文
	 * 
	 * @param aliveMsg
	 *            (byte[])
	 */
	public void setAliveMsg(byte[] aliveMsg) {
		this.aliveMsg = aliveMsg;
	}

	/**
	 * 设置心跳间隔时间
	 * 
	 * @param aliveDelayTime
	 *            (int)
	 */
	public void setAliveDelayTime(int aliveDelayTime) {
		this.aliveDelayTime = aliveDelayTime;
	}
	/**
	 * @author hp 心跳线程
	 */
	private class keepAliveThread implements Runnable {
		@Override
		public void run() {
			while (isKeepAlive) {
				while (startKeepAlive) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e1) {
						OnResponseFinish.clientException("心跳线程睡眠时被意外中断");
					}
					send(aliveMsg);// 发送心跳消息
					try {
						Thread.sleep(aliveDelayTime);// 延时
						hasKeepAliveMsg=false;
					} catch (InterruptedException e) {
						OnResponseFinish.clientException("心跳线程睡眠时被意外中断");
					}
				}
			}
		}
	}

	/**
	 * 
	 * @author hp 核心线程－－－负责消息的发送和接收
	 */
	private class CoreThread implements Runnable {
		private OnResponseFinish OnResponseFinish;

		public CoreThread(OnResponseFinish OnResponseFinish) {
			this.OnResponseFinish = OnResponseFinish;
		}

		@Override
		public void run() {
			while (isCoreRun) {
				try {
					int count = 0;
					while (count == 0) {
						byte[] msg = null;
						if (!msgQueue.isEmpty()) {
							if (outputStream != null) {
								msg = msgQueue.take();
								outputStream.write(msg);
								outputStream.flush();
							}
						}
						if (inputStream != null) {
							count = inputStream.available();// 返回输入流字节长度，用于判断是否有接收到内容
						}
						if (msg == aliveMsg) {
							hasKeepAliveMsg=true;
							OnResponseFinish.setKeepAliveMsg(hasKeepAliveMsg);//有心跳反馈
						}
					}
					OnResponseFinish.getInputStream(inputStream);// 返回一个输入流
				} catch (IOException e) {
					OnResponseFinish.clientException("收发线程IO出现异常");
				} catch (InterruptedException e) {
					OnResponseFinish.clientException("消息队列取出数据时被意外中断");
				}
			}
		}
	}

	/**
	 * 接口回调结果
	 * 
	 * @param callback
	 */
	public void getResponse(OnResponseFinish OnResponseFinish) {
		this.OnResponseFinish = OnResponseFinish;
	}

}
