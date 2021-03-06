package com.liabit.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.liabit.viewbinding.genericBinding
import com.liabit.viewbinding.inflate

/**
 * Author:         songtao
 * CreateDate:     2020/12/4 18:05
 */
abstract class BaseVBFragment<VB : ViewBinding> : BaseCompatFragment() {

    protected val binding by genericBinding<VB>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflate<VB>(this, inflater, container)
    }

}