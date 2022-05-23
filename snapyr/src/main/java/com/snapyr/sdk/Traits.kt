package com.snapyr.sdk

import android.content.Context
import java.util.*

class Traits(map: ValueMap = emptyValueMap()) : ValueMap by map {
    /**
     * Private API, users should call {@link Snapyr#identify(String)} instead. Note that this is
     * unable to enforce it, users can easily do traits.put(id, ..);
     */
    var userId: String? by stringValue("userId")
    var anonymousId: String? by stringValue("anonymousId", UUID.randomUUID().toString())

    /**
     * Returns the currentId the user is identified with. This could be the user id or the anonymous
     * ID.
     */
    fun currentId(): String? = userId ?: anonymousId

    /** Set an address for the user or group. */
    var address: ValueMap? by valueMap("address")

    fun address(): Address? = address?.asAddress()

    /** Set the age of a user. */
    var age: Int? by intValue("age", 0)

    /** Set a URL to an avatar image for the user or group. */
    var avatar: String? by stringValue("avatar")

    /** Set the user's birthday. */
    var birthday: Date? by dateValue("birthday")

    /**
     * Set the date the user's or group's account was first created. We accept date objects and a
     * wide range of date formats, including ISO strings and Unix timestamps. Feel free to use
     * whatever format is easiest for you - although ISO string is recommended for Android.
     */
    var createdAt: String? by stringValue("createdAt")

    /** Set a description of the user or group, like a personal bio. */
    var description: String? by stringValue("description")

    /** Set the email address of a user or group. */
    var email: String? by stringValue("email")

    /** Set the number of employees of a group, typically used for companies. */
    var employees: String? by stringValue("employees")

    /** Set the fax number of a user or group. */
    // todo: maybe remove this, I doubt any bundled integration uses fax
    var fax: String? by stringValue("fax")

    /** Set the first name of a user. */
    var firstName: String? by stringValue("firstName")

    /** Set the gender of a user. */
    var gender: String? by stringValue("gender")

    /** Set the industry the user works in, or a group is part of. */
    var industry: String? by stringValue("industry")

    /** Set the last name of a user. */
    var lastName: String? by stringValue("lastName")

    /** Set the name of a user or group. */
    var name: String? by stringValue("name")

    fun name(): String = name ?: "$firstName $lastName".trim()

    /** Set the phone number of a user or group. */
    var phone: String? by stringValue("phone")

    /**
     * Set the title of a user, usually related to their position at a specific company, for example
     * "VP of Engineering"
     */
    var title: String? by stringValue("title")

    /**
     * Set the user's username. This should be unique to each user, like the usernames of Twitter or
     * GitHub.
     */
    var username: String? by stringValue("username")

    /** Set the website of a user or group. */
    var website: String? by stringValue("website")
//
//    fun putValue(key: String, value: Any?): Traits {
//        this[key] = value
//        return this
//    }

    /** Represents information about the address of a user or group. */
    class Address(map: ValueMap) : ValueMap by map {
        var city: String? by stringValue("city")
        var country: String? by stringValue("country")
        var postalCode: String? by stringValue("postalCode")
        var state: String? by stringValue("state")
        var street: String? by stringValue("street")
    }

    private fun ValueMap.asAddress() = Address(this)
}


fun ValueMap.asTraits(): Traits = Traits(this)

private const val TRAITS_CACHE_PREFIX = "traits-"
fun createTraitsCache(
    context: Context,
    cartographer: Cartographer,
    tag: String
) = ValueMapCache(context, cartographer, TRAITS_CACHE_PREFIX + tag, tag)
