package com.liabit.base

import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.liabit.viewmodel.ApplicationViewModel
import com.liabit.viewmodel.genericViewModels

/**
 * Author:         songtao
 * CreateDate:     2020/12/4 18:04
 */
@Suppress("MemberVisibilityCanBePrivate")
open class BaseActivity<VM : ViewModel, VB : ViewBinding> : BaseVBActivity<VB>() {

    protected val viewModel by genericViewModels<VM>()

    override fun onBeforeInitialize() {
        val vm = viewModel
        if (vm is ApplicationViewModel) {
            vm.observeDialog(this) {
                if (it.show) {
                    showDialog(it.message)
                } else {
                    dismissDialog()
                }
            }
        }
    }

}