package org.pyneo.wirelesslocation.cellular;

import android.content.Context;
import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import org.pyneo.wirelesslocation.MainService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CellSpecRetriever {
	private static final String TAG = "org.pyneo.wirelesslocation.platform.CellSpecRetriever";
	private TelephonyManager telephonyManager;

	public CellSpecRetriever(Context context) {
		Log.d(TAG, "CellSpecRetriever: context");
		this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}

	private void addCellSpecFromCellLocation(Collection<CellSpec> cellSpecs, int mcc, int mnc, CellLocation cellLocation) {
		Log.d(TAG, "addCellSpecFromCellLocation:");
		if (cellLocation instanceof GsmCellLocation) {
			GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
			int cid = gsmCellLocation.getCid();
			int lac = gsmCellLocation.getLac();
			int psc = gsmCellLocation.getPsc();
			if (psc == -1) {
				cellSpecs.add(new CellSpec(Radio.GSM, mcc, mnc, lac, cid));
			}
			else {
				CellSpec cellSpec = new CellSpec(Radio.UMTS, mcc, mnc, lac, cid);
				cellSpec.setPsc(psc);
				cellSpecs.add(cellSpec);
			}
		}
		else if (cellLocation instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
			int sid = cdmaCellLocation.getSystemId(); // as mnc
			int nid = cdmaCellLocation.getNetworkId(); // as lac
			int bid = cdmaCellLocation.getBaseStationId(); // as cid
			cellSpecs.add(new CellSpec(Radio.CDMA, mcc, sid, nid, bid));
		}
		else {
			if (MainService.DEBUG) Log.d(TAG, "Not connected to network or using LTE, which is not supported for SDK <= 16");
			if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN)) {
				if (MainService.DEBUG) Log.d(TAG, "Android SDK > 16 features are not used yet");
			}
			// TODO:
		}
	}

	private void addCellSpecsFromNeighboringCellInfo(Collection<CellSpec> cellSpecs, int mcc, int mnc, List<NeighboringCellInfo> neighboringCellInfo) {
		Log.d(TAG, "addCellSpecsFromNeighboringCellInfo:");
		if (neighboringCellInfo == null) {
			Log.d(TAG, "addCellSpecsFromNeighboringCellInfo: no neighboringCellInfo");
			return;
		}
		for (NeighboringCellInfo cellInfo : neighboringCellInfo) {
			int lac = cellInfo.getLac();
			int cid = cellInfo.getCid();
			int psc = cellInfo.getPsc();
			int rssi = cellInfo.getRssi();
			if ((lac != NeighboringCellInfo.UNKNOWN_CID) && (cid != NeighboringCellInfo.UNKNOWN_CID)) {
				if (psc != NeighboringCellInfo.UNKNOWN_CID) {
					CellSpec cellSpec = new CellSpec(Radio.UMTS, mcc, mnc, lac, cid);
					cellSpec.setPsc(psc);
					cellSpecs.add(cellSpec);
				}
				else {
					cellSpecs.add(new CellSpec(Radio.GSM, mcc, mnc, lac, cid));
				}
			}
		}
	}

	public Collection<CellSpec> retrieveCellSpecs() {
		Log.d(TAG, "retrieveCellSpecs:");
		Collection<CellSpec> cellSpecs = new ArrayList<CellSpec>();
		if (telephonyManager != null) {
			CellLocation.requestLocationUpdate();
			String mccmnc = telephonyManager.getNetworkOperator();
			if (mccmnc != null && mccmnc.length() >= 5) {
				int mcc = Integer.parseInt(mccmnc.substring(0,3));
				int mnc = Integer.parseInt(mccmnc.substring(3));
				addCellSpecFromCellLocation(cellSpecs, mcc, mnc, telephonyManager.getCellLocation());
				addCellSpecsFromNeighboringCellInfo(cellSpecs, mcc, mnc, telephonyManager.getNeighboringCellInfo());
				if (MainService.DEBUG) {
					Log.d(TAG, "retrieveCellSpecs: Found "+cellSpecs.size()+" Cells");
					for (Object cellSpec: cellSpecs) Log.d(TAG, "retrieveCellSpecs: Found "+cellSpec);
				}
			}
		}
		return cellSpecs;
	}
}
