package io.userhabit.common.utils


import java.math.BigInteger
import java.security.MessageDigest

class FileNamePeasant {
    companion object {


        /**
         * This is used only for image file names.
         *
         * For the security problem of the MD5
         * @see https://en.wikipedia.org/wiki/MD5#Security
         *
         */
        fun md5(input: String): String {
            val md = MessageDigest.getInstance("MD5")
            val bigInt = BigInteger(1, md.digest(input.toByteArray()))
            return String.format("%032x", bigInt)
        }

        fun getFileHashId(objId: String?, scrollViewId: String?, viewId: String?): String {
            val inputStr = objId ?: scrollViewId ?: viewId ?: ""
            return md5(inputStr)
        }
    }
}