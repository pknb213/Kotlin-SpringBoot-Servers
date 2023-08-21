package io.userhabit.batch

import io.userhabit.common.Status
import io.userhabit.polaris.service.SessionService
import io.userhabit.polaris.service.geoip.GeoIpMapper
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import io.userhabit.polaris.Protocol as P

/**
 * 세션 컬랙션의 ip 값을 읽어서 iso code 값으로 변환 후 업데이트
 * ex) {"ip":"172.312.22.101"} > {"ico":"KR"}
 * @author sbnoh
 */
object IpToIsoBatch {
    val COLLECTION_NAME = "geolite_ip"

    fun tenMinutesAdv(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
        val isoCodePreloaded = "KR"
        val geoIpMapper = GeoIpMapper()
        val updateResStream = geoIpMapper.updateCountryCodeOfSessionListStream(
            isoCodePreloaded,
            fromDt.format(BatchUtil.toDateFormater),
            toDt.format(BatchUtil.toDateFormater)
        )

        return updateResStream.flatMap {
            IpToIsoBatch.tenMinutes(fromDt, toDt)
        }
    }

    fun tenMinutes(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
        val query = """
[
  { "#match": {
      "st": {
        "#gte" : {"#date": "${fromDt.format(BatchUtil.toDateFormater)}"},
        "#lt" : {"#date": "${toDt.format(BatchUtil.toDateFormater)}"}
	  },
	  "ico" : {"#exists": false}
    }
  },
  { "#lookup": {
      "from": "${COLLECTION_NAME}",
      "let": { "${P.ipi}": "#${P.ipi}" },
      "pipeline": [
        { "#match":
					{ "#expr":
						{ "#and":
							[
								{ "#lt": ["#min", "##${P.ipi}"] },
								{ "#gt": ["#max", "##${P.ipi}"] }
							]
						}
					}
        },
        { "#sort":
          {
						"max_minus_min": 1
          }
        }
      ],
      "as": "geolite"
    }
	},
	{ "#project": {
			"${P.ico}": { "#arrayElemAt": ["#geolite.${P.icoK}", 0] }
		}
	},
  {
    "#merge": {
      "into": "${SessionService.COLLECTION_NAME}"
    }
  }
]
    """
        return BatchUtil.run(object : Any() {}.javaClass, query, SessionService.COLLECTION_NAME, fromDt, toDt)
    }

    /**
     * 수동 배치 실행시 오류 방지를 위해 빈 함수로.
     */
    fun oneDay(fromDt: ZonedDateTime, toDt: ZonedDateTime): Mono<String> {
        return Mono.just("")
    }

}
