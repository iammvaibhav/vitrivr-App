package org.vitrivr.vitrivrapp.data.model.query

import org.vitrivr.vitrivrapp.data.model.enums.MessageType

data class MoreLikeThisQueryModel(val segmentId: String, val categories: ArrayList<String>, val messageType: MessageType)