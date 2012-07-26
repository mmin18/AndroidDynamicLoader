package com.dianping.example.fragmentloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListFragment;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ListApkFragment extends ListFragment {
	private List<Map<String, String>> data = new ArrayList<Map<String, String>>();

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		try {
			AssetManager asset = getActivity().getAssets();
			for (String s : asset.list("apks")) {
				addItem(s, "apks/" + s);
			}
		} catch (Exception e) {
		}

		SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
				android.R.layout.simple_list_item_1, new String[] { "title" },
				new int[] { android.R.id.text1 });
		setListAdapter(adapter);
	}

	private void addItem(String title, String path) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("title", title);
		map.put("path", path);
		data.add(map);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, String> item = data.get(position);
		String path = item.get("path");

		Intent i = new Intent("com.dianping.intent.action.LOAD_FRAGMENT");
		i.putExtra("path", path);
		i.putExtra("class", "com.dianping.example.fragment.SampleFragment");
		startActivity(i);
	}
}
