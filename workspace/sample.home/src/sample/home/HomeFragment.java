package sample.home;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.dianping.loader.MainActivity;
import com.dianping.loader.MyResources;

public class HomeFragment extends Fragment {
	private static final int REQUEST_CITY = 1;

	private TextView cityView;
	private TextView searchText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = MyResources.getResource(HomeFragment.class).inflate(
				getActivity(), R.layout.home, container, false);
		v.findViewById(R.id.shoplist).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
					}
				});
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		MainActivity ma = (MainActivity) getActivity();

		View v = MyResources.getResource(HomeFragment.class).inflate(
				getActivity(), R.layout.home_title, null, false);
		cityView = (TextView) v.findViewById(R.id.city);
		cityView.setText("Shanghai");
		cityView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});
		searchText = (TextView) v.findViewById(R.id.search);
		searchText
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH
								|| event.getAction() == KeyEvent.ACTION_DOWN
								&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
							return true;
						}
						return false;
					}
				});
		v.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
		ma.getActionBar().setDisplayShowTitleEnabled(false);
		ma.getActionBar().setDisplayShowHomeEnabled(false);
		ma.getActionBar().setDisplayShowCustomEnabled(true);
		ma.getActionBar().setCustomView(v);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CITY && resultCode == Activity.RESULT_OK) {
			// DPObject city = data.getParcelableExtra("city");
			// setCity(getActivity(), city);
			// cityView.setText(city.getString("Name"));
		}
	}
}
