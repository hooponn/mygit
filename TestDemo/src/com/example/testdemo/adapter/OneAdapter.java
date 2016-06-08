package com.example.testdemo.adapter;

import java.util.List;

import com.example.testdemo.R;
import com.example.testdemo.bean.OneList;
import com.example.testdemo.util.ImageLoader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class OneAdapter extends BaseAdapter {
		private Context context;
		private List<OneList> list;
	public OneAdapter(Context context, List<OneList> list) {
			this.context = context;
			this.list = list;
			ImageLoader.init(context);
		}
	@Override
	public int getCount() {
		return list.size();
	}
	@Override
	public OneList getItem(int position) {
		return list.get(position);
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh=null;
		if(convertView==null){
			vh=new ViewHolder();
		convertView=LayoutInflater.from(context).inflate(R.layout.onelist_item,parent,false);
		vh.title=(TextView) convertView.findViewById(R.id.onelist_title);
		vh.stTime=(TextView) convertView.findViewById(R.id.onglist_stTime);
		vh.days=(TextView) convertView.findViewById(R.id.onelist_days);
		vh.likeCount=(TextView) convertView.findViewById(R.id.onelist_likeCount);
		vh.iv=(ImageView) convertView.findViewById(R.id.onelist_img);
		convertView.setTag(vh);
		}else{
			vh=(ViewHolder) convertView.getTag();
		}
		ImageLoader.loadImage(list.get(position).getHeadImageUrl(),vh.iv);
		vh.title.setText(list.get(position).getTitle());
		vh.stTime.setText("时间:"+list.get(position).getStartTime());
		vh.days.setText("天数:"+list.get(position).getDays());
		vh.likeCount.setText("点赞数:"+list.get(position).getLikeCount());
		return convertView;
	}
	
public void refresh(List<OneList> onelist){
	list.clear();
	list.addAll(onelist);
	notifyDataSetChanged();
}	
	
private class ViewHolder{
	TextView title,url,stTime,days,likeCount;
	ImageView iv;
}










}
