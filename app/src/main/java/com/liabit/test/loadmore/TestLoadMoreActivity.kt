package com.liabit.test.loadmore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liabit.recyclerview.loadmore.LoadMoreAdapter
import com.liabit.test.R
import com.liabit.test.databinding.ActivityTestLoadMoreBinding
import com.liabit.test.mock.Mock
import com.liabit.viewbinding.inflate

class TestLoadMoreActivity : AppCompatActivity() {

    private val binding by inflate<ActivityTestLoadMoreBinding>()

    private var mAdapter: Adapter = Adapter()
    private var mLoadMoreAdapter: LoadMoreAdapter<*, *>? = null

    private var mCount = 0

    private var mDataList = ArrayList<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        mLoadMoreAdapter = LoadMoreAdapter.wrap(mAdapter).apply {
            setStyle(this@TestLoadMoreActivity, R.style.TestLoadMoreStyle)
            showNoMoreEnabled = true
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = mLoadMoreAdapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.root.postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
                mCount = 0
                mLoadMoreAdapter?.isLoadMoreEnabled = true
                mDataList.clear()
                val size = Mock.nextInt(4, 8)
                mDataList.addAll(MutableList(size) {
                    it
                })
                Log.d("TTTT", "swipeRefreshLayout ok size: $size  item count: ${mDataList.size}")
                mAdapter.setData(mDataList)
            }, 2000)
        }

        mLoadMoreAdapter?.setLoadMoreListener {
            Log.d("TTTT", "start load more -> $mCount  item count: ${mDataList.size}")
            binding.root.postDelayed({
                if (mCount < 10) {
                    if (mCount % 3 == 2) {
                        mCount++
                        Log.d("TTTT", "load more failed item count: ${mDataList.size}")
                        it.isLoadFailed = true
                    } else {
                        mCount++
                        val size = Mock.nextInt(4, 8)
                        mDataList.addAll(MutableList(size) {
                            it
                        })
                        Log.d("TTTT", "load more ok size: $size  item count: ${mDataList.size}")
                        mAdapter.setData(mDataList)
                    }
                } else {
                    Log.d("TTTT", "no more item count: ${mDataList.size}")
                    it.isEnabled = false
                }
            }, 2 * 1000L)
        }

        mAdapter.setData(mDataList)

        binding.removeLast.setOnClickListener {
            if (mDataList.isNotEmpty()) {
                mDataList.removeLast()
                mAdapter.setData(mDataList)
            }
        }
        binding.removeLastTwo.setOnClickListener {
            if (mDataList.size >= 2) {
                mDataList.removeAt(mDataList.size - 1)
                mDataList.removeAt(mDataList.size - 1)
                mAdapter.setData(mDataList)
            }
        }
        binding.removeLastThree.setOnClickListener {
            if (mDataList.size >= 3) {
                mDataList.removeAt(mDataList.size - 1)
                mDataList.removeAt(mDataList.size - 1)
                mDataList.removeAt(mDataList.size - 1)
                mAdapter.setData1(mDataList)
                mAdapter.notifyItemRangeRemoved(mDataList.size, 3)
            }
        }
        binding.rangeInsert0.setOnClickListener {
            mDataList.add(0, 23)
            mAdapter.setData1(mDataList)
            mAdapter.notifyItemRangeInserted(0, 1)
        }

    }

    class Adapter : RecyclerView.Adapter<Adapter.Holder>() {

        private var mData: List<Int> = emptyList()

        fun setData1(data: List<Int>) {
            mData = data
        }

        fun setData(data: List<Int>) {
            mData = data
            notifyDataSetChanged()
        }

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            fun setData(position: Int, data: Int) {
                itemView.findViewById<TextView>(R.id.textView)?.text = "$position: $data"
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.fragment_nestedrecyclerview_test_top_item,
                        parent, false
                    )
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.setData(position, mData[position])
        }

        override fun getItemCount(): Int {
            return mData.size
        }

    }
}