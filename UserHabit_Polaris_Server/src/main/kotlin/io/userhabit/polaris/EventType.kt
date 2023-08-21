package io.userhabit.polaris


object EventType {

	// 액션(무반응터치) = 10xx

	const val NOACT_TAP = 1001
	const val NOACT_DOUBLE_TAP = 1002
	const val NOACT_LONG_TAP = 1003
	const val NOACT_SWIPE = 1004

	const val NOACT_PAN_START = 1005	// (22.2.21 변경)
	const val NOACT_PAN_MOVE = 1006
	const val NOACT_PAN_END = 1007
	const val NOACT_SECRET_TAP = 1008	// (22.2.21 추가)

	const val NOACT_BOUNDARY = 1100		// ---- 모든 NOACT(무반응터치)는 이 값 미만


	// 액션(반응터치) = 11xx

	const val REACT_TAP = 1101
	const val REACT_DOUBLE_TAP = 1102
	const val REACT_LONG_TAP = 1103
	const val REACT_SWIPE = 1104

	const val REACT_PAN_START = 1105	// (22.2.21 변경)
	const val REACT_PAN_MOVE = 1106
	const val REACT_PAN_END = 1107
	const val REACT_SECRET_TAP = 1108	// (22.2.21 추가)

	const val TOUCH_BOUNDARY = 1200		// ---- 모든 TOUCH(무반응터치, 반응터치)는 이 값 미만


	// 액션(기기버튼) = 12xx

	const val DEVICE_BUTTON = 1201		// (기기 버튼 누름, 22.2.21 번호 변경)

	const val ACTION_BOUNDARY = 1300	// ---- 모든 ACTION(무반응터치, 반응터치, 기기버튼)는 이 값 미만


	// 이벤트(키보드창) = 14xx

	const val KEYBOARD_ENABLE = 1401
	const val KEYBOARD_DISABLE = 1400

	// 이벤트(스크롤) = 16xx

	const val SCROLL_CHANGE = 1606		// (2022.2.21 번호 변경, 1106이나 1106과 같이 들어옴)
	const val SCROLL_END = 1600			// (삭제예정)

	// 이벤트(사용자컨텐츠) = 21xx
	// 이벤트(크래쉬) = 34xx

	const val USER_CONTENT = 2101		// (2022.2.21 이름 변경, USER_OPTION -> USER_CONTENT)
	const val CRASH = 3400				// (SDK 플랫폼에 관계없이 하나로 할수 있다면, 이것만 남음)
	//const val CRASH_AOS = 3410		// (SDK 플랫폼마다 달라야 한다면)
	//const val CRASH_IOS = 3420		// (SDK 플랫폼마다 달라야 한다면, 구버전 IOS_CRASH -> 아마도 CRASH로 통합 가능)

	// 이벤트(화면) = 41xx

	const val VIEW_START = 4101			// (새로운 이름으로 화면 시작)
	const val VIEW_CHANGE = 4102		// (화면 이름은 그대로지만, 화면 이미지가 바뀜)
	const val VIEW_END = 4100			// (해당 화면 끝)

	// 이벤트(앱, 액티비티) = 8xxx

	const val APP_FOREGROUND = 8201		// (앱이 포커스를 얻고 포그라운드 상태가 됨)
	const val APP_BACKGROUND = 8200		// (앱이 포커스를 잃고 백그라운드 상태가 됨)
	const val APP_START = 8801			// (앱이 시작되거나, 사용자가 세션을 수동으로 시작함)
	const val APP_END = 8800			// (앱이 종료되거나, 사용자가 세션을 수동으로 강제 종료함)

	/** Polaris code base */
	//	/** 1001 */ const val NON_REACT_TAP = 1001
	//	/** 1002 */ const val NON_REACT_DOUBLE_TAP = 1002
	//	/** 1003 */ const val NON_REACT_LONG_TAP = 1003
	//	/** 1004 */ const val NON_REACT_SWIPE = 1004
	//
	//	/** 1101 */ const val REACT_TAP = 1101
	//	/** 1102 */ const val REACT_DOUBLE_TAP = 1102
	//	/** 1103 */ const val REACT_LONG_TAP = 1103
	//	/** 1104 */ const val REACT_SWIPE = 1104
	//
	//	/** 1201 */ const val PAN_START = 1201
	//	/** 1202 */ const val PAN_MOVE = 1202
	//	/** 1200 */ const val NON_REACT_PAN_END = 1200
	//	/** 1300 */ const val REACT_PAN_END = 1300
	//
	//	/** 1401 */ const val KEYBOARD_ENABLE = 1401
	//	/** 1400 */ const val KEYBOARD_DISABLE = 1400
	//
	//	/** 1601 */ const val SCROLL_CHANGE = 1601 // merge 2021.3.22
	//	/** 1600 */ const val SCROLL_END = 1600 // merge 2021.3.22 (삭제예정)
	//
	//	/** 1701 */ const val SECRET_TAP = 1701 // 시크릿모드에서 제스쳐
	//
	//	/** 1801 */ const val DEVICE_BUTTON = 1801 // (물리키, 소프트키) 기기 버튼 누름
	//
	//	/** 4101 */ const val VIEW_START = 4101
	//	/** 4100 */ const val VIEW_END = 4100
	//
	//	/** 8801 */ const val APP_START = 8801
	//	/** 8800 */ const val APP_END = 8800
	//	/** 8201 */ const val APP_FOREGROUND = 8201 // merge 2021.3.22
	//	/** 8200 */ const val APP_BACKGROUND = 8200 // merge 2021.3.22
	//
	//	/** 2101 */ const val USER_OPTION = 2101 // merge 2021.3.22
	//	/** 3400 */ const val CRASH = 3400 // merge 2021.3.22 (클라 플랫폼에 관계없이 하나로 할수 있다면)
	//	/** 3410 */ //const val CRASH_AOS = 3410 // merge 2021.3.22 (클라 플랫폼마다 달라야 한다면)
	//	/** 3420 */ //const val CRASH_IOS = 3420 // merge 2021.3.22 (클라 플랫폼마다 달라야 한다면, 구버전 IOS_CRASH -> 아마도 CRASH로 통합 가능)

	/** Anejo code base */
	//	const val NON_REACT_TAP = 8191
	//	const val NON_REACT_DOUBLE_TAP = 0
	//	const val NON_REACT_LONG_TAP = 0
	//	const val NON_REACT_SWIPE = 0
	//
	//	const val REACT_TAP = 8192
	//	const val REACT_DOUBLE_TAP = 8193
	//	const val REACT_LONG_TAP = 8194
	//	const val REACT_SWIPE = 8195
	//
	//	const val KEYBOARD_ENABLE = 8201
	//	const val KEYBOARD_DISABLE = 8202
	//	const val SCROLL_CHANGE = 12290
	//	const val SCROLL_END = 12291 // //삭제 예정?
	//
	//	const val VIEW_START = 4353
	//	const val VIEW_END = 4354
	//
	//	const val APP_START = 4096
	//	const val APP_END = 4097
	//	const val APP_FOREGROUND = 4672
	//	const val APP_BACKGROUND = 4608
	//
	//	const val USER_OPTION = 20484
	//	const val CRASH = 20485
	//	const val IOS_CRASH = 20483

	private val codeMap = mapOf<Int, Int>(
		/*0x1fff*/	8191	to NOACT_TAP,

		/*0x2000*/	8192	to REACT_TAP,
		/*0x2001*/	8193	to REACT_DOUBLE_TAP,
		/*0x2002*/	8194	to REACT_LONG_TAP,
		/*0x2003*/	8195	to REACT_SWIPE,

		/*0x2007*/	8199	to DEVICE_BUTTON,	// (Anejo Menu Btn)
		/*0x2008*/	8200	to DEVICE_BUTTON,	// (Anejo Back Btn)
		/*0x2009*/	8201	to KEYBOARD_ENABLE,
		/*0x200A*/	8202	to KEYBOARD_DISABLE,
		/*0x3002*/	12290	to SCROLL_CHANGE,
		/*0x3003*/	12291	to SCROLL_END,

		/*0x1101*/	4353	to VIEW_START,
		/*0x1102*/	4354	to VIEW_END,		// (Anejo Android SDK에 없음)

		/*0x1000*/	4096	to APP_START,
		/*0x1001*/	4097	to APP_END,
		/*0x1240*/	4672	to APP_FOREGROUND,	// (Anejo Android SDK에 없음)
		/*0x1200*/	4608	to APP_BACKGROUND,

		/*0x5004*/	20484	to USER_CONTENT,
		/*0x5005*/	20485	to CRASH,
		/*0x5003*/	20483	to CRASH,
	)

	/**
	 * From Anejo EventType to Polaris
	 */
	fun convertToPolaris(code: Int): Int{
		return codeMap.getOrDefault(code, code)
	}
}

