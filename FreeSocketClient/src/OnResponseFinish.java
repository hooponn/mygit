

import java.io.InputStream;

/**
 * 回调接口
 * 
 * @author 返回一个输入流
 */
public interface OnResponseFinish {
	/**
	 * 获得一个输入流
	 * 
	 * @param in
	 */
	void getInputStream(InputStream in);

	/**
	 * 异常处理
	 */
	void clientException(String errMsg);
	/**
	 * 心跳状态反馈和动态设置心跳消息
	 */
	void setKeepAliveMsg(boolean isAlive);
}

