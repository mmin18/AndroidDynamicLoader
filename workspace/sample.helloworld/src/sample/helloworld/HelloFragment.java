package sample.helloworld;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dianping.app.MyApplication;
import com.dianping.loader.MyResources;

public class HelloFragment extends Fragment {
	String name;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// MyResources manages the resources in specific package.
		// Using a Class object to obtain an instance of MyResources.

		// In this case, hello.xml is in the same package as HelloFragment class

		MyResources res = MyResources.getResource(HelloFragment.class);

		// Using MyResources.inflate() if you want to inflate some layout in
		// this package.
		return res.inflate(getActivity(), R.layout.hello, container, false);

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (savedInstanceState != null) {
			name = savedInstanceState.getString("name");
		}
		update();

		view.findViewById(R.id.start_url).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {

						// Start the PickerFragment by url mapping.
						// (app://pickname is mapped to PickerFragment, defined
						// in fragment.properties)
						Intent i = new Intent(Intent.ACTION_VIEW, Uri
								.parse(MyApplication.PRIMARY_SCHEME
										+ "://pickname?selection=" + name));

						// We need a result, the result will be callback in
						// onActivityResult()
						startActivityForResult(i, 1);
					}
				});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("name", name);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
			name = data == null ? null : data.getStringExtra("selection");
			update();
		}
	}

	private void update() {
		TextView tv = (TextView) getView().findViewById(R.id.text_hello);

		if (TextUtils.isEmpty(name)) {
			tv.setText("Hello World!");
		} else {
			tv.setText("Hello " + name + "!");
		}
	}

}
