package sample.helloworld;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

/**
 * app://picker?selection=Ryan
 * <p>
 * params:<br>
 * selection : string
 * <p>
 * result:<br>
 * selection : string
 * 
 * @author mmin18
 * 
 */
public class PickerFragment extends ListFragment implements
		AdapterView.OnItemClickListener {
	String[] ITEMS = { "Alex", "Andy", "Ben", "Carl", "Denny", "Edward",
			"Howard", "Ivan", "Jimmy", "Kevin", "Larry", "Mark", "Nicholas",
			"Paul", "Ryan", "Steven", "Tommy", "Vincent" };

	String selection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = getActivity().getIntent().getData();
		selection = uri.getQueryParameter("selection");
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setListAdapter(new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, ITEMS) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				if (ITEMS[position].equals(selection)) {
					v.setBackgroundColor(0x40FF0000);
				} else {
					v.setBackgroundColor(0);
				}
				return v;
			}
		});

		getListView().setOnItemClickListener(this);

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		Intent data = new Intent();
		data.putExtra("selection", ITEMS[position]);
		getActivity().setResult(Activity.RESULT_OK, data);
		getActivity().finish();

	}

}
