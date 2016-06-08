package com.example.testdemo.ui;

import java.util.ArrayList;
import java.util.List;

import com.example.testdemo.R;
import com.example.testdemo.adapter.OneAdapter;
import com.example.testdemo.bean.OneList;
import com.example.testdemo.listener.OnOneListFinishListener;
import com.example.testdemo.util.HttpUtil;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainActivity extends Activity {
ListView lv;
List<OneList> list;
OneAdapter oAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		lv=(ListView) findViewById(R.id.lv_one);
		list=new ArrayList<OneList>();
		oAdapter=new OneAdapter(this, list);
		lv.setAdapter(oAdapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
			}
		});
	}
@Override
	protected void onResume() {
		super.onResume();
		HttpUtil.getOneListRequest("³¤É³",2,new OnOneListFinishListener() {
			@Override
			public void refreshList(List<OneList> onelist) {
				oAdapter.refresh(onelist);
			}
		});
	}
}
