package joandersongoncalves.example.veganocook.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import joandersongoncalves.example.veganocook.R
import joandersongoncalves.example.veganocook.data.model.Category
import joandersongoncalves.example.veganocook.presentation.adapter.RecipeAdapter
import joandersongoncalves.example.veganocook.presentation.viewmodel.CategoryViewModel
import kotlinx.android.synthetic.main.activity_category.*
import kotlinx.android.synthetic.main.app_toolbar.*
import kotlinx.android.synthetic.main.recipe_filters.*

class CategoryActivity : AppCompatActivity() {

    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // setting RecyclerView
        val recipeAdapter = RecipeAdapter { recipe ->
            val intent = RecipeDetailsActivity.getStartIntent(this@CategoryActivity, recipe)
            val reqCode = 1
            this@CategoryActivity.startActivityForResult(intent, reqCode)
        }
        with(rvRecipesByCategory) {
            layoutManager = GridLayoutManager(this@CategoryActivity, 2)
            setHasFixedSize(true)
            adapter = recipeAdapter
        }

        // setting viewModel
        viewModel.recipesByCategory.observe(this, Observer {
            recipeAdapter.setRecipes(it)
            if (it.isEmpty()) { //the results returned zero recipes
                viewFlipperCategoryActivity.displayedChild = 1
            } else { //results returned at least one recipe
                viewFlipperCategoryActivity.displayedChild = 0
            }
        })
        //setting activity title
        viewModel.categoryTitle.observe(this, Observer {
            appToolbarOther.title = it
        })
        intent.getStringExtra(EXTRA_CATEGORY_TITLE)?.let {
            when (it) {
                getString(R.string.favorite) -> {
                    viewModel.isFavoriteRecipesOnly.value = true
                    viewModel.updateAllRecipes()
                }
                else -> {
                    viewModel.checkedChangeOnSelectedCategory(Category(it))
                }
            }
            viewModel.categoryTitle.value = it
        }


        //setting the right toolbar for this activity
        viewFlipperAppToolbar.displayedChild = 1
        appToolbarOther.title = intent.getStringExtra(EXTRA_CATEGORY_TITLE)
        AppToolbarSetup.setBackButton(appToolbarOther, this)

        //setting drawer
        drawerLayoutCategoryActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        //setting button Filters
        textOpenFilters.setOnClickListener {
            drawerLayoutCategoryActivity.openDrawer(drawerFilters)
            addAllCategoriesIntoChips()
        }

        //setting button Clean All
        drawerFilters
            .getHeaderView(0)
            .findViewById<Button>(R.id.btCleanSelectionFiltersDrawer)
            .setOnClickListener {
                //clean filter selection
                viewModel.cleanSelectedCategories()
                //recreate all category chips
                addAllCategoriesIntoChips()
            }

        viewModel.selectedCategoriesOnFilter.observe(this, Observer {
            //now filter categories
            viewModel.updateAllRecipes()
        })

        viewModel.getAllCategories()
    }

    private fun addAllCategoriesIntoChips() {
        viewModel.allCategories.value?.let {
            chipGroupCategoriesFilter.removeAllViews()
            for (category in it) {
                val categoryChip = layoutInflater.inflate(
                    R.layout.chip_all_categories,
                    chipGroupCategoriesFilter,
                    false
                ) as Chip
                viewModel.selectedCategoriesOnFilter.value?.let { selectedCategories ->
                    if (selectedCategories.contains(category)) {
                        categoryChip.isChecked = true
                    }
                }
                categoryChip.setOnCheckedChangeListener { _, _ ->
                    viewModel.checkedChangeOnSelectedCategory(category)
                }
                categoryChip.text = category.categoryName
                chipGroupCategoriesFilter.addView(categoryChip)
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayoutCategoryActivity.isDrawerOpen(drawerFilters)) {
            drawerLayoutCategoryActivity.closeDrawers()
        } else {
            super.onBackPressed()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RecipeDetailsActivity.RETURN_UPDATED_RECIPE) {
            viewModel.updateAllRecipes()
        }
        if (resultCode == RecipeDetailsActivity.DELETE_RECIPE) {
            //snackbar confirming exclusion:
            Snackbar.make(
                baseLayoutCategoryActivity,
                R.string.success_deleting_recipe,
                Snackbar.LENGTH_SHORT
            ).show()
        }
        viewModel.getAllCategories() //updating categories for any possible updates
    }

    companion object {
        private const val EXTRA_CATEGORY_TITLE = "EXTRA_CATEGORY_TITLE"

        fun getStartActivity(context: Context, title: String): Intent {
            return Intent(context, CategoryActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY_TITLE, title)
            }
        }
    }
}
