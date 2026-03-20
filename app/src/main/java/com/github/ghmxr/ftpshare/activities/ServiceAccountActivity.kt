package com.github.ghmxr.ftpshare.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import com.github.ghmxr.ftpshare.Constants
import com.github.ghmxr.ftpshare.MyApplication
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.adapers.AccountListAdapter
import com.github.ghmxr.ftpshare.services.FtpService
import com.github.ghmxr.ftpshare.utils.CommonUtils
import com.github.ghmxr.ftpshare.utils.StorageAccessUtil
import com.google.android.material.checkbox.MaterialCheckBox

class ServiceAccountActivity : BaseActivity() {
    companion object {
        private const val REQUEST_ANONYMOUS_TREE = 0
    }

    private var viewGroup_anonymous: ViewGroup? = null
    private var accountListView: ListView? = null
    private var viewGroup_no_account: ViewGroup? = null
    private var anonymous_path: TextView? = null
    private var writable_cb: MaterialCheckBox? = null
    private var menu: Menu? = null

    private val settings = CommonUtils.getSettingSharedPreferences(MyApplication.getGlobalBaseContext())
    private val editor = settings.edit()

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.fragment_account)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources?.getString(R.string.item_account_settings)
        viewGroup_anonymous = findViewById<ViewGroup>(R.id.mode_anonymous)
        accountListView = findViewById<ListView>(R.id.view_user_list)
        anonymous_path = findViewById<TextView>(R.id.mode_anonymous_value)
        writable_cb = findViewById<MaterialCheckBox>(R.id.anonymous_writable_cb)
        viewGroup_no_account = findViewById<ViewGroup>(R.id.add_user_att)

        findViewById<View>(R.id.anonymous_path).setOnClickListener(this::onClick)
        findViewById<View>(R.id.anonymous_writable).setOnClickListener(this::onClick)
        refreshContents()
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.anonymous_path -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this)
                    return
                }
                startActivityForResult(
                    StorageAccessUtil.createOpenDocumentTreeIntent(
                        settings.getString(
                            Constants.PreferenceConsts.ANONYMOUS_MODE_TREE_URI,
                            Constants.PreferenceConsts.ANONYMOUS_MODE_TREE_URI_DEFAULT
                        )
                    ),
                    REQUEST_ANONYMOUS_TREE
                )
            }
            R.id.anonymous_writable -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this)
                    return
                }
                writable_cb?.toggle()
                editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE, writable_cb!!.isChecked)
                editor.apply()
            }
            else -> {
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_service_account, menu)
        this.menu = menu
        menu?.let {
            it.getItem(0).setVisible(!CommonUtils.isAnonymousMode(this));
            it.getItem(1).title = if (CommonUtils.isAnonymousMode(this)) resources.getString(R.string.action_main_anonymous_opened) else resources.getString(R.string.action_main_anonymous_closed)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_main_add -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this);
                    return true;
                }
                startActivityForResult(Intent(this, AddAccountActivity::class.java), 1)
                return true;
            }
            R.id.action_main_anonymous_switch -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this)
                    return true
                }
                try {
                    val settings = getSharedPreferences(Constants.PreferenceConsts.FILE_NAME, MODE_PRIVATE)
                    val editor = settings.edit()
                    val isAnonymousMode = settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE, Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT)
                    editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE, !isAnonymousMode)
                    editor.apply()
                    menu?.getItem(1)?.setTitle(if (!isAnonymousMode) resources.getString(R.string.action_main_anonymous_opened) else resources.getString(R.string.action_main_anonymous_closed))
                    menu?.getItem(0)?.setVisible(isAnonymousMode)
                    refreshContents()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshContents() {
        anonymous_path?.text = StorageAccessUtil.getDirectorySummary(
            this,
            settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_TREE_URI, Constants.PreferenceConsts.ANONYMOUS_MODE_TREE_URI_DEFAULT),
            settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH, Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT)
        )
        writable_cb?.isChecked = settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE, Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE_DEFAULT)
        val accountListAdapter = AccountListAdapter(this, accountListView!!)
        accountListView?.adapter = accountListAdapter
        viewGroup_anonymous?.visibility = if (CommonUtils.isAnonymousMode(this)) View.VISIBLE else View.GONE
        accountListView?.visibility = if (CommonUtils.isAnonymousMode(this)) View.GONE else View.VISIBLE
        viewGroup_no_account?.visibility = if (CommonUtils.isAnonymousMode(this)) View.GONE else if (accountListAdapter.accountItems.size > 0) View.GONE else View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ANONYMOUS_TREE && resultCode == RESULT_OK) {
            data?.data?.let {
                StorageAccessUtil.persistTreePermission(this, it, data.flags)
                editor.putString(Constants.PreferenceConsts.ANONYMOUS_MODE_TREE_URI, it.toString())
                editor.putString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH, "")
                editor.apply()
            }
        }
        refreshContents()
    }
}
