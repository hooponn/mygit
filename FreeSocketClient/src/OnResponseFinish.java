

import java.io.InputStream;

/**
 * �ص��ӿ�
 * 
 * @author ����һ��������
 */
public interface OnResponseFinish {
	/**
	 * ���һ��������
	 * 
	 * @param in
	 */
	void getInputStream(InputStream in);

	/**
	 * �쳣����
	 */
	void clientException(String errMsg);
	/**
	 * ����״̬�����Ͷ�̬����������Ϣ
	 */
	void setKeepAliveMsg(boolean isAlive);
}

