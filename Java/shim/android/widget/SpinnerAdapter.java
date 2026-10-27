package android.widget;
import android.view.View;
import android.view.ViewGroup;
public interface SpinnerAdapter extends Adapter {
    default View getDropDownView(int position, View convertView, ViewGroup parent) { return getView(position, convertView, parent); }
}
