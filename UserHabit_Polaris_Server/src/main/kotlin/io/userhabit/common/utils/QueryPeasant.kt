package io.userhabit.common.utils

import io.userhabit.common.Config
import io.userhabit.polaris.service.StorageService
import org.bson.Document
import org.bson.types.ObjectId

class QueryPeasant {
    companion object {
        private val s3Bucket = Config.get("s3.bucket")
        private val storageType = Config.get("storage.type")
        private val storagePath = Config.get("storage.path")
        fun convertToObjectIdList(strVals: List<String>): List<ObjectId> {
            return strVals.map { value ->
                ObjectId(value)
            }
        }

        /**
         * This is for mongodb query
         *
         * @return
         * {
         *   "$concat" : [
         *     "$storage.sty",
         *     "//service-uh-polaris-stage-2/attach/",
         *     "$storage.ai", "/", "$storage.av", "/", "$storage.fhi"
         *   ]
         * }
         */
        fun getImageURI(): Document {
            var mid = "//"
            if (storageType == "s3:") {
                // for S3, the absoulte and relative pathes are the same
                val spath = this._removeFirstSlash(storagePath)
                mid = "//${s3Bucket}/${spath}/"
            }
            val appIdStr = Document("\$toString", "\$storage.ai")
            return Document("\$concat", listOf("\$storage.sty", mid, appIdStr, "/", "\$storage.av", "/", "\$storage.fhi"))

        }

        fun listDocToStr(listDocs: List<Document>): String{
            val queryStr = "[" + listDocs.joinToString(separator = ",", transform = {
                it.toJson()
            }) + "]"
            return queryStr
        }

        /**
         * > _removeFirstSlash("/attach")
         * 'attach'
         * > _removeFirstSlash("attach")
         * 'attach'
         */
        fun _removeFirstSlash(path: String): String{
            return if(path.startsWith("/")) path.substring(1) else path
        }
    }
}