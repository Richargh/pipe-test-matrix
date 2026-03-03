package de.richargh.pipematrix.api

import com.gitlab.api.GitLabApiException
import com.gitlab.api.GitLabClient
import de.richargh.pipematrix.domain.BranchName
import de.richargh.pipematrix.domain.PipelineId
import de.richargh.pipematrix.domain.ProjectPath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

