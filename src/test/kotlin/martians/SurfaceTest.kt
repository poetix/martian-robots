package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import martians.domain.LeaveScent
import martians.domain.Position
import java.util.concurrent.ConcurrentHashMap

class SurfaceTest : FunSpec({

    test("safety comes from the terrain") {
        val surface = Surface(grid5x3)
        surface.isSafe(5 at 3) shouldBe true
        surface.isSafe(6 at 3) shouldBe false
    }

    test("leaving a scent is perceived from then on") {
        val surface = Surface(grid5x3)
        surface.hasScent(2 at 3) shouldBe false
        surface.apply(LeaveScent(2 at 3))
        surface.hasScent(2 at 3) shouldBe true
    }

    test("the scent store is the caller's choice, e.g. a concurrent set for parallel runners") {
        val scents = ConcurrentHashMap.newKeySet<Position>()
        Surface(grid5x3, scents).apply(LeaveScent(2 at 3))
        scents shouldBe setOf(2 at 3)
    }
})
