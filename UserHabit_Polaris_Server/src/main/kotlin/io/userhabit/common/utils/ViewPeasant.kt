package io.userhabit.common.utils

class ViewPeasant {
    companion object {
        public val VIEWID_SESSIONSTART = "###SESSION_START###"
        public val VIEWID_SESSIONEND = "###SESSION_END###"

        /**
         * > getVhi("###SESSION_START###")
         * 74180013
         * > getVhi("###SESSION_END###")
         * -1671032428
         */
        fun getVhi(vi: String): Int {
            return vi.hashCode()
        }

        fun getVhiOfSessionStart(): Int {
            return getVhi(VIEWID_SESSIONSTART)
        }

        fun getVhiOfSessionEnd(): Int {
            return getVhi(VIEWID_SESSIONEND)
        }
    }
}