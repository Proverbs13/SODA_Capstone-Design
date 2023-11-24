/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soda.soda.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soda.soda.databinding.ItemProbabilityBinding
import com.soda.soda.helper.AudioClassificationHelper
import com.soda.soda.helper.TextMatchingHelper
import org.tensorflow.lite.support.label.Category

class ProbabilitiesAdapter : RecyclerView.Adapter<ProbabilitiesAdapter.ViewHolder>() {
    // 분류 결과를 저장하기 위한 List<Category> 변수
    var categoryList: List<Category> = emptyList()
        set(value) {
            field = value.filterNot { isExcluded(it.label) } // excludedLabel 필터링
            notifyDataSetChanged()
        }

    // ViewHolder 생성 함수
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // ViewHolder에 사용할 레이아웃 파일을 inflate하여 ViewHolder 객체를 생성
        val binding =
            ItemProbabilityBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
        return ViewHolder(binding)
    }
    // ViewHolder를 특정 position에 바인딩하는 함수
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // categoryList에서 특정 position의 카테고리 정보를 가져옴
        val category = categoryList[position]

        holder.bind(TextMatchingHelper.textMatch(category), category.score, category.index)
    }

    // ViewHolder의 개수를 반환하는 함수
    override fun getItemCount(): Int {
        return categoryList.size
    }

    // ViewHolder 클래스
    class ViewHolder(private val binding: ItemProbabilityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        // ViewHolder에 데이터를 바인딩하는 함수
        fun bind(label: String, score: Float, index: Int) {
            with(binding) {
                // 레이아웃 파일에서 labelTextView라는 View를 가져와서 메시지 값을 설정함
                labelTextView.text = label

                // 확률 값을 ProgressBar의 진행 상태로 표시함
                val newValue = (score * 100).toInt()
                progressBar.progress = newValue
            }
        }
    }

    private fun isExcluded(label: String) : Boolean {
        Log.d("SurroundCustomFragment", "$label")
        Log.d("SurroundCustomFragment", "${AudioClassificationHelper.excludedLabel}")
        Log.d("SurroundCustomFragment", "excludedLabel: ${AudioClassificationHelper.excludedLabel.contains(label)}")
        return AudioClassificationHelper.excludedLabel.contains(label)
    }

}
