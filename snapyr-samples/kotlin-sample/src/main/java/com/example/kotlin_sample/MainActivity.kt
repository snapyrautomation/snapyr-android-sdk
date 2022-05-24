/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.example.kotlin_sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlin_sample.databinding.ActivityMainBinding
import com.snapyr.sdk.Properties
import com.snapyr.sdk.Snapyr
import com.snapyr.sdk.Traits
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        initialSetup()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.docs -> {
                val openDocs = Intent(Intent.ACTION_VIEW)
                openDocs.data = Uri.parse("https://snapyr.com/docs/")
                startActivity(openDocs)
                try {
                    startActivity(openDocs)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "No browser to open link", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.reset -> {
                Snapyr.with(this).reset()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onAButtonClick() {
        val props = Properties()
        props.putName("capture")
        props.put("move", "exd6")
        props.put("orgId", "f95991da-ea9c-4e44-998d-5ff5f4ca04d3")
        props.put("workspaceId", "d00f0649-c6a4-475c-8eeb-518ae5f29768")
        Snapyr.with(this).track("capture", props)
        Toast.makeText(this, "Button A clicked", Toast.LENGTH_SHORT).show()
    }

    private fun onBButtonClick() {
        Snapyr.with(this).track("Button B clicked")
        Toast.makeText(this, "Button B clicked", Toast.LENGTH_SHORT).show()
    }

    private fun onIdentifyClick() {
        val name = identify_name.text.toString()
        val email = identify_email.text.toString()
        val userId = identify_user_id.text.toString()
        val allFieldsEmpty = isNullOrEmpty(name) && isNullOrEmpty(email) && isNullOrEmpty(userId)

        if (allFieldsEmpty) {
            Toast.makeText(this, "At least one field must be filled in", Toast.LENGTH_SHORT).show()
        } else {
            if (!isNullOrEmpty(userId)) {
                Snapyr.with(this).identify(userId)
            }
            if (!isNullOrEmpty(name)) {
                Snapyr.with(this).identify(Traits().apply { this.name = name })
            }
            if (!isNullOrEmpty(email)) {
                Snapyr.with(this).identify(Traits().apply { this.email = email })
            }

            Toast.makeText(this, "Identification acknowledged", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onGroupClick() {
        val groupId = group_id.text.toString()

        if (isNullOrEmpty(groupId)) {
            Toast.makeText(this, "Cannot have an empty group id", Toast.LENGTH_SHORT).show()
        } else {
            Snapyr.with(this).group(groupId)
            Toast.makeText(this, "Group acknowledged", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onScreenClick() {
        val screenIntent = Intent(this, ScreenActivity::class.java)
        startActivity(screenIntent)
        Toast.makeText(this, "Screen acknowledged", Toast.LENGTH_SHORT).show()
    }

    private fun onAliasClick() {
        val aliasCopy = alias_text.text.toString()

        if (isNullOrEmpty(aliasCopy)) {
            Toast.makeText(this, "Cannot have an empty alias", Toast.LENGTH_SHORT).show()
        } else {
            Snapyr.with(this).alias(aliasCopy)
            Snapyr.with(this).identify(aliasCopy)
            Toast.makeText(this, "Alias acknowledged", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onFlushClick() {
        Snapyr.with(this).flush()
        Toast.makeText(this, "Events flushed", Toast.LENGTH_SHORT).show()
    }

    private lateinit var binding: ActivityMainBinding

    private fun initialSetup() {
        binding.actionTrackA.setOnClickListener { onAButtonClick() }
        binding.actionTrackB.setOnClickListener { onBButtonClick() }
        binding.identifyButton.setOnClickListener { onIdentifyClick() }
        binding.groupButton.setOnClickListener { onGroupClick() }
        binding.aliasButton.setOnClickListener { onAliasClick() }
        binding.screenButton.setOnClickListener { onScreenClick() }
        binding.flush.setOnClickListener { onFlushClick() }
    }

    /** Returns true if the string is null, or empty (when trimmed).  */
    private fun isNullOrEmpty(text: String): Boolean {
        return TextUtils.isEmpty(text) || text.trim { it <= ' ' }.isEmpty()
    }
}
