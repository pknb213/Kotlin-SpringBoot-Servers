package io.userhabit.polaris.service

import io.userhabit.common.*
import reactor.core.publisher.Mono
import reactor.util.Loggers
import java.util.*


object CountryService {
	private val log = Loggers.getLogger(this.javaClass)
//	const val COLLECTION_NAME = "app"


	/**
	 * @author sbnoh
	 */
	fun get(req: SafeRequest): Mono<Map<String, Any>> {
		val countryIdList = req.splitParamOrDefault("ids","")

		val fieldList = req.splitQueryOrDefault("field_list", "code_country,code_language")
		val sortField = req.getQueryOrDefault("sort_field", "created_date")
		val sortValue = req.getQueryOrDefault("sort_value", 1)
		val skip = req.getQueryOrDefault("skip",0)
		val limit = req.getQueryOrDefault("limit", 100)
		val searchField = req.getQueryOrDefault("search_field","")
		val searchValue = req.getQueryOrDefault("search_value","")

		val validator = Validator()
			.new(limit, "limit").max(1000)

		if (validator.isNotValid())
			throw PolarisException.status400BadRequest(validator.toExceptionList())

		val country = Locale.getAvailableLocales()
			.filter{
				if(countryIdList.isNotEmpty()){
					countryIdList.contains(it.country)
				} else {
					it.country.isNotEmpty() && it.country.elementAt(0) > '@' // from 'A'
				} &&
				if(searchField.isNotEmpty()){
					if     (searchField == "tag" )              it.toLanguageTag().contains(searchValue)
					else if(searchField == "display_country" )  it.displayCountry.contains(searchValue)
					else if(searchField == "display_language" ) it.displayLanguage.contains(searchValue)
					else true
				} else {
					true
				}
			}
			.let {
				if(sortValue == -1) it.sortedByDescending {
					if     (searchField == "tag" )            it.toLanguageTag()
					else if(sortField == "display_country" )  it.displayCountry
					else if(sortField == "display_language" ) it.displayLanguage
					else it.country
				}
				else it.sortedBy {
					if     (searchField == "tag" )            it.toLanguageTag()
					else if(sortField == "display_country" )  it.displayCountry
					else if(sortField == "display_language" ) it.displayLanguage
					else it.country
				}
			}
			.filterIndexed { index, locale ->
				skip <= index && index < (skip + limit)
			}
			.map {
				val country = mutableMapOf<String, String>()
				if(fieldList.contains("tag") ) country["tag"] = it.toLanguageTag()
				if(fieldList.contains("display_country") ) country["display_country"] = it.displayCountry
				if(fieldList.contains("display_language") ) country["display_language"] = it.displayLanguage
				country
			}

		return Mono
			.just(country)
			.map{
				Status.status200Ok(it)
			}

	} // end of get()
}

