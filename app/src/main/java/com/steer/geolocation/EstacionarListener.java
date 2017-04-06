package com.steer.geolocation;

import android.view.View;
import android.widget.Toast;

/**
 * Created by Pedro on 22/09/2016.
 */

public class EstacionarListener implements View.OnClickListener {

    @Override
    public void onClick(View view) {
        Toast.makeText(view.getContext(),"Usted ha estacionado",Toast.LENGTH_LONG).show();
    }


}