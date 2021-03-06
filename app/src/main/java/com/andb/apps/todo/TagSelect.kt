package com.andb.apps.todo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.andb.apps.todo.eventbus.AddTaskAddTagEvent
import com.andb.apps.todo.filtering.Filters
import com.andb.apps.todo.data.model.Tag
import com.andb.apps.todo.utilities.Current
import com.andb.apps.todo.utilities.ProjectsUtils
import com.github.rongi.klaster.Klaster
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity
import kotlinx.android.synthetic.main.content_tag_select.*
import kotlinx.android.synthetic.main.tag_list_item.view.*
import org.greenrobot.eventbus.EventBus


class TagSelect : CyaneaAppCompatActivity() {


    private var isTaskCreate = false

    private var isTagLink = false


    private var contextualToolbar: ActionMode? = null
    lateinit var list: List<Tag>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_select)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        list = Current.tagListAll()
        Log.d("tagSort", "keys: ${list.map { it.id }}")
        Log.d("tagSort", "indices: ${list.map { it.index }}")


        val bundle = intent.extras
        if (bundle!!.containsKey("isTaskCreate")) {
            isTaskCreate = bundle.getBoolean("isTaskCreate")
        }

        if (intent.hasExtra("isTagLink")) {
            isTagLink = bundle.getBoolean("isTagLink")

        }

        val tagRecycler = tagrecycler
        tagRecycler.layoutManager = LinearLayoutManager(this)


        // specify an adapter (see also next example)
        mAdapter = tagAdapter(list)
        tagRecycler.adapter = mAdapter


        tagRecycler.addOnItemTouchListener(RecyclerTouchListener(applicationContext, tagRecycler, object :
            RecyclerTouchListener.ClickListener {
            override fun onClick(view: View, position: Int) {
                val tags = list[position]
                Toast.makeText(applicationContext, tags.name + " is selected!", Toast.LENGTH_SHORT)
                    .show()


                if (isTaskCreate) {

                    EventBus.getDefault().post(AddTaskAddTagEvent(position))
                    finish()
                } else if (isTagLink && Filters.getCurrentFilter().size > 0) {
                    val tag = Filters.getMostRecent()
                    tag.children.apply {
                        when (contains(position)) {
                            false -> {
                                if (position != tag.index) {
                                    add(position)
                                    Filters.updateMostRecent(tag)
                                    ProjectsUtils.update(tag)
                                    finish()
                                } else {
                                    Snackbar.make(toolbar.rootView, "The tag you selected is the current tag", Snackbar.LENGTH_LONG)
                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show()
                                }
                            }
                            true -> Snackbar.make(toolbar.rootView, "The tag you selected has already been linked", Snackbar.LENGTH_LONG).setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).show()
                        }
                    }
                } else {
                    Filters.tagForward(list[position])
                    finish()
                }

            }

            override fun onLongClick(view: View, position: Int) {

                contextualToolbar = this@TagSelect.startActionMode(setCallback(position))
                view.isSelected = true
            }


        }))


    }

    private fun tagAdapter(list: List<Tag>) = Klaster.get()
        .itemCount { list.size }
        .view(R.layout.tag_list_item, layoutInflater)
        .bind { position ->
            val tag = list[position]
            itemView.tagname.text = tag.name
            itemView.tagIcon.setColorFilter(tag.color)
        }
        .build()


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_tag_select, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_bar_add -> {
                MaterialDialog(this).show{
                    val
                }
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }


    fun setCallback(position: Int): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val menuInflater = MenuInflater(this@TagSelect.applicationContext)
                menuInflater.inflate(R.menu.toolbar_tag_select_long_press, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.editTag -> {
                        val editTask = Intent(this@TagSelect.applicationContext, CreateTag::class.java)
                        editTask.putExtra("edit", true)
                        editTask.putExtra("editPos", position)
                        startActivity(editTask)
                        mode.finish()
                        return true
                    }
                    R.id.deleteTag -> {
                        ProjectsUtils.remove(list[position])
                        mAdapter.notifyItemRemoved(position)
                        mode.finish()
                    }
                }

                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        }

    }


    public override fun onPause() {
        super.onPause()
    }


    companion object {
        lateinit var mAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    }


}
