package com.liabit.location

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.MyLocationStyle
import com.liabit.location.extension.checkAndRequestPermission
import com.liabit.location.extension.checkAppPermission
import com.liabit.location.extension.location_permissions
import com.liabit.location.model.PoiAddress
import kotlinx.android.synthetic.main.fragment_location_picker.*
import kotlinx.android.synthetic.main.picker_toolbar.*

/**
 *
 */
class LocationPickerFragment : BaseFragment(), LocationBottomSheetDialog.OnItemClickListener, Toolbar.OnMenuItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    private val viewModel: LocationViewModel? by lazy {
        context?.let {
            return@lazy LocationViewModel(it.applicationContext)
        }
    }

    private var poiSearcher: PoiSearcher? = null

    private var myLocation: Location? = null

    private lateinit var mapProxy: MapProxy

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_location_picker, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity == null) return
        enableOptionsMenu(toolbar, false, R.menu.location_picker)
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        toolbar.setOnMenuItemClickListener(this)

        activity?.let { poiSearcher = PoiSearcher(it) }

        mapView.onCreate(savedInstanceState)
        if (!checkPermission()) {
            checkAndRequestPermission(*location_permissions)
        }

        //初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        // 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
        myLocationStyle.showMyLocation(true)

        mapView.map.setMyLocationStyle(myLocationStyle)
        mapView.map.isMyLocationEnabled = true

        mapProxy = MapLocationFactory.create(requireContext(), mapView = mapView)

        searchBox.setOnEditorActionListener { textView, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val keyword = textView.text?.toString() ?: ""
                    if (keyword.isNotEmpty()) {
                        search(keyword)
                    }
                    return@setOnEditorActionListener true
                }
                else -> {
                    return@setOnEditorActionListener false
                }
            }
        }

        viewModel?.location?.observe(viewLifecycleOwner, Observer {
            myLocation = it
            setMyLocation(it, 18f)
        })

        myLocationButton.setOnClickListener {
            getMyLocation()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.confirm -> {
                searchBox.text?.toString()?.let {
                    search(it)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun search(keyword: String) {
        if (myLocation != null) {
            myLocation?.let {
                poiSearcher?.search(it, keyword)?.observe(viewLifecycleOwner, Observer { result ->
                    showPoiSearchResult(keyword, result)
                })
            }
        } else {
            viewModel?.getMyLocation { location ->
                myLocation = location
                poiSearcher?.search(location, keyword)?.observe(viewLifecycleOwner, Observer { result ->
                    showPoiSearchResult(keyword, result)
                })
            }
        }
    }

    private fun showPoiSearchResult(title: String, result: List<PoiAddress>) {
        val list = ArrayList<PoiAddress>()
        list.addAll(result)
        val fragment = childFragmentManager.findFragmentByTag("poi_result")
        if (fragment != null) {
            val sheet = fragment as LocationBottomSheetDialog
            sheet.setSearchResult(result)
            sheet.setTitle(title)
        } else {
            LocationBottomSheetDialog.newInstance(Bundle().apply {
                putParcelableArrayList(Constants.Extra.LIST, list)
                putString(Constants.Extra.TITLE, title)
            }).apply {
                setOnItemClickListener(this@LocationPickerFragment)
            }.show(childFragmentManager, "poi_result")
        }
    }

    override fun onItemClick(view: View, position: Int, address: PoiAddress) {
        Log.d("LocationPickerFragment", "position: $position  address: $address")
    }

    private fun getMyLocation() {
        viewModel?.getMyLocation()
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun setMyLocation(location: Location, zoomLevel: Float?) {
        if (location.latitude != null && location.longitude != null) {
            //var latLng = convertGpsToGCJ02(location.latitude!!, location.longitude!!)
            //使用高德定位之后，国内默认是GCJ02
            val latLng = LatLng(location.latitude, location.longitude)
            if (zoomLevel != null) {
                mapView.map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
            } else {
                mapView.map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        mapView.onDestroy()
        super.onDestroyView()
    }


    private fun checkPermission(): Boolean {
        return context?.checkAppPermission(*location_permissions) == true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (checkPermission()) {

        }
    }

    companion object {
        @JvmStatic
        fun newInstance(arguments: Bundle? = null) = LocationPickerFragment().apply {
            this.arguments = arguments
        }
    }


}