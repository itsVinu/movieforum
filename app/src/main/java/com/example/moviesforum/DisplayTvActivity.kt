package com.example.moviesforum

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.moviesforum.Model.TvModel.casttvresponse.CastItem
import com.example.moviesforum.Model.Wishes
import com.example.moviesforum.adapter.castadapter.CastTvAdapter
import com.example.moviesforum.client.Client
import com.google.android.material.navigation.NavigationView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_display.detailIv
import kotlinx.android.synthetic.main.main_display.detailIv2
import kotlinx.android.synthetic.main.main_display.detailTv1
import kotlinx.android.synthetic.main.main_display.detailTv2
import kotlinx.android.synthetic.main.main_display.detailTv3
import kotlinx.android.synthetic.main.main_display.mainDispRv
import kotlinx.android.synthetic.main.main_display.toolbar1
import kotlinx.android.synthetic.main.main_display_tv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisplayTvActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val list = arrayListOf<CastItem>()
    val casttvadapter = CastTvAdapter(list)

    val db by lazy {
        Room.databaseBuilder(this,
            AppDatabase::class.java,
            "app.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    var trigger = MutableLiveData<Boolean>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_tv)


        setSupportActionBar(toolbar1)

        val tv_id = intent.getStringExtra("tvid")

        trailerBtnTv.setOnClickListener{
            val intent = Intent(this,TrailerTvActivity::class.java)
            intent.putExtra("tvid",tv_id)
            startActivity(intent)
        }


        GlobalScope.launch {
            val response = withContext(Dispatchers.IO) { Client.api.getAllDetailsTv(tv_id)}
            if (response.isSuccessful){
                response.body()?.let {
                    runOnUiThread{
                        detailTv1.text = it.originalName
                        detailTv2.text = it.firstAirDate
                        detailTv3.text = it.overview
                        Picasso.get().load("https://image.tmdb.org/t/p/w500" + it.posterPath.toString()).into(detailIv)
                        Picasso.get().load("https://image.tmdb.org/t/p/w500" + it.posterPath.toString()).into(detailIv2)
                    }
                }
            }
        }

        mainDispRv.apply {
            layoutManager = LinearLayoutManager(this@DisplayTvActivity, RecyclerView.HORIZONTAL, false)
            adapter = casttvadapter
        }

        GlobalScope.launch {
            val response = withContext(Dispatchers.IO) { Client.api.getAllCastTv(tv_id) }
            if (response.isSuccessful) {
                response.body()?.let { res ->
                    res.cast?.let {
                        list.addAll(it)
                    }
                    runOnUiThread { casttvadapter.notifyDataSetChanged() }
                }
            }
        }

        var lists = arrayListOf<Long>()

        wishlistBtnTv.setOnClickListener {
//            add.visibility = View.INVISIBLE
//            add.setBackgroundColor(android.R.color.holo_blue_bright)

            val i = Intent(this,WishlistTvActivity::class.java)
            trigger.value = false

            GlobalScope.launch {
                val response = withContext(Dispatchers.IO){Client.api.getAllDetailsTv(tv_id)}

                if (response.isSuccessful){
                    response.body()?.let {
                        GlobalScope.launch(Dispatchers.Main) {
                            val a = withContext(Dispatchers.IO){
                                lists = db.wishesdao().getAllUsersTvId() as ArrayList<Long>
                                if (lists.contains(tv_id.toLong())){
                                    GlobalScope.launch(Dispatchers.Main) {
                                        Toast.makeText(this@DisplayTvActivity,"present in wishlist", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else{
                                    db.wishesdao().insert(
                                        Wishes(
                                            it.name.toString(),
                                            it.originalName.toString(),
                                            it.posterPath.toString(),
                                            it.overview.toString(),
                                            it.id?.toLong()
                                        )
                                    )
                                    GlobalScope.launch(Dispatchers.Main) {
                                        Toast.makeText(this@DisplayTvActivity,"Added To Wishlist", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            i.putExtra("id",tv_id)
        }

        val toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar1,
            R.string.open,
            R.string.close
        )

        drawer.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.movie -> {
                startActivity(Intent(this,MainActivity::class.java))
                // Toast.makeText(this,"MOVIE", Toast.LENGTH_SHORT).show()
            }
            R.id.tvSeries ->{
                startActivity(Intent(this,Main2Activity::class.java))
            }
            R.id.discover ->{
                startActivity(Intent(this,Main3Activity::class.java))
            }
            R.id.wishlistMovie ->{
                startActivity(Intent(this,WishlistMovieActivity::class.java))
            }
            R.id.wishlistTvSeries ->{
                startActivity(Intent(this,WishlistTvActivity::class.java))
            }
        }
        drawer.closeDrawer(GravityCompat.START)   //used to close the navigation drawer when the items inside the drawer are clicked
        return true
    }
}
