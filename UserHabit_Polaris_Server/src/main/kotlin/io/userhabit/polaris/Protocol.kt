package io.userhabit.polaris

object Protocol {
	/**AppKey*/                   const val ak   = "ak"; const val akK   = "app_key"
	/**AppBuild*/                 const val ab   = "ab"
	/**AppVersion*/               const val av   = "av"; const val avK   = "app_version"
	/**UserhabitSdkVersion*/      const val usv = "usv"; const val usvK  = "userhabit_sdk_version"
	/**ServerInputTime???*/       const val i   = "i"

	/**DeviceCompany*/            const val dc   = "dc"
	/**DeviceBoard*/              const val db   = "db"
	/**DeviceId*/                 const val di   = "di"; const val diK   = "device_id"
	/**DeviceModel*/              const val dm   = "dm"
	/**DeviceName*/               const val dn   = "dn"; const val dnK   = "device_name"
	/**DeviceWidth*/              const val dw   = "dw"; const val dwK   = "device_w"
	/**DeviceHeight*/             const val dh   = "dh"; const val dhK  = "device_h"
	/**DeviceWidthHeight*/        const val dwh  = "dwh"; const val dwhK  = "device_wh"
	/**DeviceDpi*/                const val dd   = "dd"
	/**DeviceOsVersion*/          const val dov  = "dov"; const val dovK  = "device_os_ver"
	/**DeviceLanguage*/           const val dl   = "dl"; const val dlK   = "device_lang"
	/**DeviceZone*/               const val dz   = "dz"; const val dzK   = "device_zone"
	/**DeviceOs*/                 const val `do` = "do"; const val doK = "device_os"

	/**SessionId*/                const val si   = "si"; const val siK   = "session_id"
	/**SessionTime*/              const val st   = "st"; const val stK   = "session_time"
	/**SessionTimeZoned*/         const val stz  = "stz"; const val stzK  = "session_time"
	/**SessionExperience*/        const val se   = "se"; const val seK   = "session_exp"
	/**SessionNetwork*/           const val sn   = "sn"; const val snK   = "session_net"

	/**Type*/                     const val t    = "t"; const val tK    = "event_type"
	/**TimeStamp*/                const val ts   = "ts"; const val tsK   = "time_stamp"

	/**ViewId*/                   const val vi   = "vi"; const val viK   = "view_id"
	/** @deprecated ViewOrientation*/  const val vo   = "vo"; const val voK   = "view_orientation"
	/**ViewWidth*/                const val vw   = "vw"; const val vwK   = "view_width"
	/**ViewHeight*/               const val vh   = "vh"; const val vhK   = "view_height"
	/**ViewAspectRatio*/          const val va   = "va"; const val vaK   = "view_aspect" // Int(1000 * vh / vw)

	/**GestureX*/                 const val gx   = "gx"; const val gxK   = "gesture_x"
	/**GestureY*/                 const val gy   = "gy"; const val gyK   = "gesture_y"
	/**finGerId*/                 const val gi   = "gi"; const val giK   = "gesture_id"

	/**GestureEndX*/              const val gex  = "gex"; const val gexK  = "gesture_end_x"
	/**GestureEndY*/              const val gey  = "gey"; const val geyK  = "gesture_end_y"
	/**ScrollPointX*/             const val spx  = "spx"
	/**ScrollPointY*/             const val spy  = "spy"
	/**GestureVector*/            const val gv   = "gv"; const val gvK   = "gesture_vector"
	/**ScrollViewId*/			  const val svi  = "svi"; const val sviK   = "scroll_view_id"
	/**ScrollViewId*/			  const val svhi = "svhi"; const val svhiK   = "scroll_view_hashed_id"
	/**ScrollViewNumber*/		  const val svn  = "svn"; const val svnK   = "scroll_view_number"

	/**KeyCode*/                  const val kc  = "kc"

	/**OptionDataKey*/            const val odk  = "odk"
	/**OptionDataValue*/          const val odv  = "odv" // 일단 "comment"나 "Description"도 여기에 작성

	/// 2022.2.21 수정 전 {
	///**CrashType*/                const val ct   = "ct"; const val ctK   = "crash_type"
	///**CrashMessage*/             const val cm   = "cm"; const val cmK   = "crash_message"
	///**CrashStacktrace*/          const val cs   = "cs"; const val csK   = "crash_stacktrace"
	///**CrashBattery*/             const val cb   = "cb"; const val cbK   = "crash_battery"
	///**CrashCharging*/            const val cc   = "cc"; const val ccK  = "crash_charge"
	///**CrashNetwork*/             const val cn   = "cn"; const val cnK   = "crash_network"
	///**CrashProcess*/             const val cp   = "cp"; const val cpK   = "crash_process"
	///**CrashFreeMemory*/          const val cfm  = "cfm"; const val cfmK  = "crash_free_memory"
	///**CrashTotalMemory*/         const val ctm  = "ctm"; const val ctmK  = "crash_memory"
	///**CrashFreeDisk*/            const val cfd  = "cfd"; const val cfdK  = "crash_free_disk"
	///**CrashTotalDisk*/           const val ctd  = "ctd"; const val ctdK  = "crash_disk"
	// } 2022.2.21 수정 전

	/// 2022.2.21 수정 후 (wasave) {
	/**CrashType*/                const val ct   = "ct"; const val ctK   = "crash_type"
	/**CrashThread*/              const val ct2   = "ct2"; const val ct2K   = "crash_thread" // anejo에선 메인 스레드 크래쉬만 전송되었지만, polaris는 모든 스레드의 크래쉬가 전송될 수 있다
	/**CrashMessage*/             const val cm   = "cm"; const val cmK   = "crash_message"
	/**CrashStacktrace*/          const val cs   = "cs"; const val csK   = "crash_stacktrace"

	/**CrashBatteryPercent*/      const val cb   = "cb"; const val cbK   = "crash_battery" // + "_percent"
	/**CrashBatteryCharging*/     const val cc   = "cc"; const val ccK  = "crash_charge" // "_charge" -> "_battery_charge"
	/**CrashNetworkStatus*/       const val cn   = "cn"; const val cnK   = "crash_network" // + "_status"
	/**CrashProcessCount*/        const val cp   = "cp"; const val cpK   = "crash_process" // + "_count"

	/**CrashFreeMemory*/          const val cfm  = "cfm"; const val cfmK  = "crash_free_memory"
	/**CrashTotalMemory*/         const val ctm  = "ctm"; const val ctmK  = "crash_memory"
	/**CrashFreeDisk*/            const val cfd  = "cfd"; const val cfdK  = "crash_free_disk"
	/**CrashTotalDisk*/           const val ctd  = "ctd"; const val ctdK  = "crash_disk"
	// } 2022.2.21 수정 후

	/**KillswitchFlag*/           const val kf   = "kf"
	/**KillswitchTime*/           const val kt   = "kt"
	/**KillswitchSdkVersion*/     const val ksv  = "ksv"
	/**KillswitchAppVersion*/     const val kav  = "kav"

	// Server side
	/** Id */					  const val id = "_id"; const val idK = "_id"
	/**AppId*/                    const val ai   = "ai"; const val aiK   = "app_id"
	/**AppCount*/                 const val aco  = "aco"; const val acoK  = "app_count"

	/**SessionCount*/             const val sco  = "sco"; const val scoK  = "session_count"
	/**SessionCrashCount*/        const val scco = "scco"; const val sccoK = "session_crash_count"
	/**AfterSessionExperience*/   const val ase  = "ase"

	/**TerminationCount*/         const val tco  = "tco"; const val tcoK  = "termination_count"
	/**CrashCount*/               const val cco  = "cco"; const val ccoK  = "crash_count"
	/**CrashId*/                  const val ci   = "ci"; const val ciK   = "crash_id"
	/**CrashMessageGroup*/        const val cg   = "cg"

	/**DeviceCrashCount*/         const val dco = "dco"; const val dcoK = "device_count"
	/**DeviceCrashCount*/         const val dcco = "dcco"; const val dccoK = "device_crash_count"

	/**ViewCount*/                const val vco  = "vco"; const val vcoK  = "view_count"
	/**UniqueViewCount*/          const val uvco = "uvco"; const val uvcoK = "unique_view_count"
	/**ViewHashId*/               const val vhi  = "vhi"; const val vhiK  = "view_hash_id"
	/**BeforeViewHashId*/         const val bvhi = "bvhi"; const val bvhiK = "before_view_hash_id"
	/**AfterViewHashId*/          const val avhi = "avhi"; const val avhiK = "after_view_hash_id"

	/**DwellTime*/                const val dt   = "dt"; const val dtK   = "dwell_time"

	/**UniqueTimeStamp*/          const val uts  = "uts"; const val utsK  = "unique_time_stamp"

	/**Event*/                    const val e    = "e"
	/**EventCount*/               const val eco  = "eco"; const val ecoK  = "event_count"

	/**DirectionTop*/             const val dit  = "dit"; const val ditK = "direction_top_count"
	/**DirectionRight*/           const val dir  = "dir"; const val dirK = "direction_right_count"
	/**DirectionBottom*/          const val dib  = "dib"; const val dibK = "direction_bottom_count"
	/**DirectionLeft*/            const val dil  = "dil"; const val dilK = "direction_left_count"

	/**FlowCount*/                const val fco  = "fco"; const val fcoK  = "flow_count"
	/**MoveCount*/                const val mco  = "mco"; const val mcoK  = "move_count"
	/**AfterMoveCount*/           const val amco = "amco"
	/**BeforeMoveCount*/          const val bmco = "bmco"

	/**ObjectId*/                 const val oi   = "oi"; const val oiK   = "object_id"
	/**ObjectHashId*/             const val ohi  = "ohi"; const val ohiK  = "object_hash_id"
	/**ObjectDescription*/        const val od   = "od"; const val odK = "object_Description";
	/**ObjectCount*/              const val oco  = "oco"; const val ocoK  = "object_count"

	/**UserId*/                   const val ui  = "ui"; const val uiK  = "member_id"
	/**UserCount*/				  const val uco = "uco"; const val ucoK = "user_count"
	/**UserName*/				  const val un  = "un"; const val unK = "name" // Todo: Modified "user_name" About login_history
	/**UserCompany*/			  const val uc  = "uc"; const val ucK = "company_id" // Todo: Modified "user_company" About login_history

	/**Created Date*/             const val cd  = "cd"; const val cdK  = "created_date"
	/**Created Date*/             const val ud  = "ud"; const val udK  = "updated_date"
	/**login Date*/				  const val ld  = "ld"; const val ldK  = "login_date"
	/**login Count*/              const val lco = "lco"; const val lcoK  = "login_count"
	/**login Count*/              const val laco = "laco"; const val lacoK  = "login_account_count"
	/**plan Type*/				  const val ptK  = "plan_type"
	
	/**add Company Count*/		  const val accK  = "add_company_count"
	/**add Company Count MoM*/	  const val accmK = "add_company_count_mom"

	/**storage type*/             const val sty  = "sty"; const val styK  = "storage_type"
	/**content type*/             const val cty  = "cty"; const val ctyK  = "content_type"
	/**file name*/                const val fn   = "fn"; const val fnK  = "file_name"
	/**file name*/                const val fhi  = "fhi"; const val fhiK  = "file_hash_id"
	/**file size*/                const val fs   = "fs"; const val fsK  = "file_size"

	/**rectangle size*/           const val rt   = "rt"; const val rtK  = "rectangle_transform"
	/**nick_name*/                const val nin  = "nin"; const val ninK  = "nick_name"
	/**ak_validation*/            const val akv  = "akv"; const val akvK  = "ak_validation"
	/**db_result*/                const val dbr  = "dbr"; const val dbrK  = "db_result"
	/**session_db_result*/        const val sdr  = "sdr"; const val sdrK  = "session_db_result"
	/**event_db_result*/          const val edr  = "edr"; const val edrK  = "event_db_result"

	/**email_auth_key*/           const val eak  = "eak"; const val eakK  = "email_auth_key"

	/** internet_protocol */      const val ip  = "ip";

	/** ip_integer */             const val ipi  = "ipi";
	/** iso_code */               const val ico  = "ico"; const val icoK  = "iso_code";

	// Batch side

	/**AllDeviceCount*/           const val adco  = "adco"; const val adcoK  = "all_device_count"
	/**NewDeviceCount*/           const val ndco  = "ndco"; const val ndcoK  = "new_device_count"
	/**ReDeviceCount*/            const val rdco  = "rdco"; const val rdcoK  = "re_device_count"

	/**CrashSessionCount*/        const val csco  = "csco"; const val cscoK  = "crash_session_count"
	/**CrashDeviceCount*/         const val cdco  = "cdco"; const val cdcoK  = "crash_device_count"

	/**RepeatCount*/              const val rco  = "rco"; const val rcoK  = "repeat_count"

	private val abbrMap = mapOf(
		adcoK  to adco ,
		idK	   to id, // Todo add
		aiK    to ai   ,
		avK    to av   ,
		avhiK  to avhi ,
		bvhiK  to bvhi ,
		cbK    to cb   ,
		ccK    to cc   ,
		ccoK   to cco  ,
		cdK    to cd  ,
		cfdK   to cfd  ,
		cfmK   to cfm  ,
		ciK    to ci   ,
		cmK    to cm   ,
		cnK    to cn   ,
		cpK    to cp   ,
		csK    to cs   ,
		ctK    to ct   ,
		ctdK   to ctd  ,
		ctmK   to ctm  ,
		ctyK   to cty ,
		dccoK  to dcco ,
		dcoK   to dco  ,
		diK    to di  ,
		dibK   to dib  ,
		dilK   to dil  ,
		dirK   to dir  ,
		ditK   to dit  ,
		dlK    to dl   ,
		dnK    to dn   ,
		doK    to `do` ,
		dovK   to dov  ,
		dtK    to dt   ,
		dwhK   to dwh  ,
		dzK    to dz   ,
		ecoK   to eco  ,
		fnK    to fn  ,
		fhiK   to fhi  ,
		fsK    to fs  ,
		gexK   to gex  ,
		geyK   to gey  ,
		gvK    to gv   ,
		gxK    to gx   ,
		gyK    to gy   ,
		giK    to gi   ,
		lacoK  to laco,
		lcoK   to lco ,
		ldK    to ld  ,
		mcoK   to mco  ,
		ndcoK  to ndco,
		ocoK   to oco  ,
		oiK    to oi   ,
		odK    to od   ,
		rdcoK  to rdco,
		sccoK  to scco ,
		scoK   to sco  ,
		seK    to se   ,
		siK    to si   ,
		snK    to sn   ,
		stK    to st   , // conflict, this key does NOT work because of the 'stzK'
		styK   to sty ,
		stzK   to stz  ,
		svhiK  to svhi ,
		sviK   to svi  ,
		svnK   to svn  ,
		tK     to t    ,
		tcoK   to tco  ,
		tsK    to ts   ,
		udK    to ud  ,
		usvK   to usv  ,
		utsK   to uts  ,
		uvcoK  to uvco ,
		vcoK   to vco  ,
		vwK    to vw   ,
		vhK    to vh   ,
		vhiK   to vhi  ,
		viK    to vi   ,
		voK    to vo   ,
	)

	fun getAbbreviation(name: String): String{
		return abbrMap[name] ?: name
	}

	/**
	 * TODO : this is temporary solution for the same key name, 'session_time'
	 * In this function, key 'session_time' returns the 'st' instead of 'stz'
	 */
	fun getAbbreviation2(name: String): String{
		if(name == stK){
			return st
		}
		return abbrMap[name] ?: name
	}

	private val fullnameMap = mapOf(
		id   to idK, // Todo add
		adco to adcoK ,
		ai   to aiK   ,
		av   to avK   ,
		avhi to avhiK ,
		bvhi to bvhiK ,
		cb   to cbK   ,
		cc   to ccK   ,
		cco  to ccoK  ,
		cd   to cdK   ,
		cfd  to cfdK  ,
		cfm  to cfmK  ,
		ci   to ciK   ,
		cm   to cmK   ,
		cn   to cnK   ,
		cp   to cpK   ,
		cs   to csK   ,
		ct   to ctK   ,
		ctd  to ctdK  ,
		ctm  to ctmK  ,
		cty  to ctyK  ,
		dcco to dccoK ,
		dco  to dcoK  ,
		di   to diK   ,
		dib  to dibK  ,
		dil  to dilK  ,
		dir  to dirK  ,
		dit  to ditK  ,
		dl   to dlK   ,
		dn   to dnK   ,
		`do` to doK   ,
		dov  to dovK  ,
		dt   to dtK   ,
		dwh  to dwhK  ,
		dz   to dzK   ,
		eco  to ecoK  ,
		fn   to fnK   ,
		fhi  to fhiK  ,
		fs   to fsK   ,
		gex  to gexK  ,
		gey  to geyK  ,
		gv   to gvK   ,
		gx   to gxK   ,
		gy   to gyK   ,
		gi   to giK   ,
		laco to lacoK ,
		lco  to lcoK  ,
		ld   to ldK   ,
		mco  to mcoK  ,
		ndco to ndcoK ,
		oco  to ocoK  ,
		oi   to oiK   ,
		od   to odK   ,
		rdco to rdcoK ,
		scco to sccoK ,
		sco  to scoK  ,
		se   to seK   ,
		si   to siK   ,
		sn   to snK   ,
		st   to stK   ,
		sty  to styK  ,
		stz  to stzK  ,
		svhi to svhiK ,
		svi  to sviK  ,
		svn  to svnK  ,
		t    to tK    ,
		tco  to tcoK  ,
		ts   to tsK   ,
		ud   to udK   ,
		usv  to usvK  ,
		uts  to utsK  ,
		uvco to uvcoK ,
		vco  to vcoK  ,
		vh   to vhK   ,
		vhi  to vhiK  ,
		vi   to viK   ,
		vo   to voK   ,
	)

	fun getFullname(abbr: String): String{
		return fullnameMap[abbr] ?: abbr
	}
}
