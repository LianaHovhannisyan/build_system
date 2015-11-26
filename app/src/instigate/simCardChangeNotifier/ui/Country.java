package instigate.simCardChangeNotifier.ui;

import java.lang.String;;

public class Country extends Object implements Comparable<Country> {
	private String mPhoneCode;
	private String mIsoCode;
	private String mName;
	private int mFlag;

	public String getPhoneCode() {
		return mPhoneCode;
	}

	public void setPhoneCode(String mPhoneCode) {
		this.mPhoneCode = mPhoneCode;
	}

	public String getIsoCode() {
		return mIsoCode;
	}

	public void setIsoCode(String mIsoCode) {
		this.mIsoCode = mIsoCode;
	}

	public String getName() {
		return mName;
	}

	public void setName(String mName) {
		this.mName = mName;
	}

	public int getFlag() {
		return mFlag;
	}

	public void setFlag(int mFlag) {
		this.mFlag = mFlag;
	}

	public Country(String phoneCode, String isoCode, String name, int flag) {
		mPhoneCode = phoneCode;
		mIsoCode = isoCode;
		mName = name;
		mFlag = flag;
	}

	public Country() {
	}

	@Override
	public int compareTo(Country another) {
		return this.getName().compareToIgnoreCase(another.getName());
	}

}
