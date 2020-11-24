package joandersongoncalves.example.koes.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import joandersongoncalves.example.koes.R
import joandersongoncalves.example.koes.data.model.Category
import joandersongoncalves.example.koes.data.model.Recipe
import joandersongoncalves.example.koes.presentation.fragment.DialogErrorNoCategoryFragment
import joandersongoncalves.example.koes.presentation.viewmodel.CreateRecipeViewModel
import kotlinx.android.synthetic.main.activity_create_recipe.*
import kotlinx.android.synthetic.main.app_toolbar.*
import kotlinx.android.synthetic.main.include_recipe_categories_create_recipe.*
import kotlinx.android.synthetic.main.include_recipe_name_create_recipe.*
import kotlinx.android.synthetic.main.include_save_favorite_create_recipe.*
import kotlinx.android.synthetic.main.include_video_description_create_recipe.*
import kotlinx.android.synthetic.main.include_youtube_link_create_recipe.*
import java.io.Serializable


@Suppress("ControlFlowWithEmptyBody")
class CreateRecipeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_recipe)

        settingToolbar()

        val viewModel = settingViewModel()

        var recipe = gettingReceivedData(viewModel)



        settingCancelButton()

        //pressing show/hide description button
        btSeeHideAllVideoDescription.setOnClickListener {
            if (tvVideoDescription.maxLines == 5) {
                tvVideoDescription.maxLines = 300
                btSeeHideAllVideoDescription.text = getString(R.string.hide_video_description)
            } else {
                tvVideoDescription.maxLines = 5
                btSeeHideAllVideoDescription.text = getString(R.string.show_video_description)
            }
        }

        //pressing delete video description button
        btDeleteVideoDescription.setOnClickListener {
            tvVideoDescription.setText("")
            tvVideoDescription.visibility = View.GONE
            layoutVideoDescriptionButtons.visibility = View.GONE
            btGetVideoDescription.visibility = View.VISIBLE
        }

        //pressing get description button
        btGetVideoDescription.setOnClickListener {
            tvVideoDescription.setText(viewModel.videoLiveData.value?.description)
            tvVideoDescription.visibility = View.VISIBLE
            layoutVideoDescriptionButtons.visibility = View.VISIBLE
            btGetVideoDescription.visibility = View.GONE
        }

        //pressing save button
        settingSaveButton(recipe, viewModel)


        // setting search button
        btSearchYoutubeLink.setOnClickListener { handleYoutubeLinkSearch(viewModel) }

        //setting add to favorite
        viewModel.favorite.observe(this, Observer {
            checkBoxAddFavorite.isChecked = it
        })
        layoutAddFavoriteCreateRecipe.setOnClickListener {
            viewModel.changeFavoriteState()
        }

        //handling pressing enter on softkeyboard on textInputYoutubeLink
        val listener = OnEditorActionListener { _, actionId, event ->
            if (event == null) {
                if (actionId == EditorInfo.IME_ACTION_DONE) ;
                else if (actionId == EditorInfo.IME_ACTION_NEXT) ;
                else return@OnEditorActionListener false // Let system handle all other null KeyEvents
            } else if (actionId == EditorInfo.IME_NULL) {
                if (event.action == KeyEvent.ACTION_DOWN) ;
                else return@OnEditorActionListener true // We consume the event when the key is released.
            } else return@OnEditorActionListener false

            //when enter is pressed do:
            handleYoutubeLinkSearch(viewModel)

            true
        }
        etYoutubeLinkCreateRecipe.setOnEditorActionListener(listener)

        //setting observer to add categories to Chips
        viewModel.recipeCategories.observe(this, Observer { listCategory ->
            layoutChipCategoryCreateRecipe.removeAllViews()
            for (category in listCategory) {

                val newCategoryView = layoutInflater.inflate(
                    R.layout.chip_category_create_recipe,
                    layoutChipCategoryCreateRecipe,
                    false
                ) as TextView

                newCategoryView.text = category.categoryName
                newCategoryView.setOnClickListener(
                    getClickListenerForCategoryChips(newCategoryView, viewModel)
                )
                layoutChipCategoryCreateRecipe.addView(newCategoryView)
            }
        })

        //handling pressing add category button
        btAddCategoryCreateRecipe.setOnClickListener {
            viewModel.recipeCategories.value?.let {
                val addCategoriesFragment =
                    AddCategoryDialogFragment(this, viewModel)
                val fm = supportFragmentManager
                addCategoriesFragment.show(fm, "add category fragment")
            }
        }
    }

    private fun getClickListenerForCategoryChips(
        textView: TextView,
        viewModel: CreateRecipeViewModel
    ): View.OnClickListener {

        return View.OnClickListener {

            viewModel.removeCategoryFromSelected(Category(textView.text.toString()))
            Snackbar.make(
                viewFlipperCreateRecipeActivity, R.string.category_removed,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.undo) {
                    viewModel.addCategoryIntoSelected(Category(textView.text.toString()))
                }
                .show()
        }
    }

    private fun settingSaveButton(recipeArg: Recipe?, viewModel: CreateRecipeViewModel): Recipe? {
        var recipe = recipeArg
        btCreateRecipeSave.setOnClickListener {
            if (validateData()) {
                val worked: Boolean
                val idMessage: Int
                //if it's editing an existing recipe
                if (recipe != null) {
                    recipe = viewModel.updateRecipe(
                        etRecipeNameCreateRecipe.text.toString(),
                        tvVideoDescription.text.toString(),
                        recipe!!
                    )
                    idMessage = R.string.success_updating_recipe

                    val intent = Intent()
                    intent.putExtra(AppConstantCodes.UPDATED_RECIPE, recipe)
                    intent.putExtra(
                        AppConstantCodes.UPDATED_RECIPE_CATEGORIES,
                        viewModel.recipeCategories.value?.toList() as Serializable
                    )
                    setResult(Activity.RESULT_OK, intent)
                } else {
                    worked = viewModel.saveNewRecipe(
                        etRecipeNameCreateRecipe.text.toString(),
                        tvVideoDescription.text.toString()
                    )
                    idMessage = if (worked) {
                        R.string.success_saving_recipe
                    } else {
                        R.string.error_saving_recipe
                    }
                }

                Toast.makeText(this, idMessage, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        return recipe
    }

    private fun settingCancelButton() {
        btCreateRecipeCancel.setOnClickListener {
            // close activity
            finish()
        }
    }

    private fun gettingReceivedData(viewModel: CreateRecipeViewModel): Recipe? {
        var recipe = intent.getParcelableExtra<Recipe>(AppConstantCodes.EXTRA_RECIPE)
        recipe?.let { rec ->
            fillFields(
                rec.name,
                rec.description,
                "https://youtu.be/${rec.video.url}",
                rec.isFavorite
            )
            viewModel.favorite.value = rec.isFavorite
            //categories:
            viewModel.getCategoriesFromRecipe(rec)
        }
        return recipe
    }

    private fun settingViewModel(): CreateRecipeViewModel {
        val viewModel: CreateRecipeViewModel by viewModels()

        viewModel.videoLiveData.observe(this, Observer {
            it?.let {
                fillFields(it.title, it.description, "https://youtu.be/${it.url}", null)
            }
        })
        viewModel.videoRetrieveResponseLiveData.observe(this, Observer {
            it?.let {
                when (it) {
                    CreateRecipeViewModel.ERROR_INVALID_LINK -> {
                        etYoutubeLinkCreateRecipe.error = getString(R.string.error_invalid_link)
                    }
                    CreateRecipeViewModel.ERROR_RETRIEVING_INFORMATION -> {
                        Snackbar.make(
                            viewFlipperCreateRecipeActivity, R.string.error_retrieving_information,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    CreateRecipeViewModel.ERROR_CONNECTING_API -> {
                        Snackbar.make(
                            viewFlipperCreateRecipeActivity,
                            R.string.error_conecting_api,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
        return viewModel
    }

    private fun settingToolbar() {
        viewFlipperAppToolbar.displayedChild = 1
        //appToolbarGeneral.setTitle(R.string.add_recipe)
        tvAppToolbarOtherTitle.text = getString(R.string.add_recipe)
        AppToolbarSetup.setBackButton(appToolbarGeneral, this)
    }

    private fun handleYoutubeLinkSearch(viewModel: CreateRecipeViewModel) {
        val link = etYoutubeLinkCreateRecipe.text
        if (link.toString().startsWith("https://youtu.be/", false)) {
            //its a valid link
            //remove prefix
            val videoLink = link.toString().removePrefix("https://youtu.be/")
            viewModel.getVideo(videoLink, getString(R.string.youtube_api_key))
        } else {
            //invalid link
            etYoutubeLinkCreateRecipe.error = getString(R.string.error_invalid_link)
        }
    }


    private fun validateData(): Boolean {

        //test title field
        if (etRecipeNameCreateRecipe.text == null || etRecipeNameCreateRecipe.text.toString() == "") {
            //invalid
            etRecipeNameCreateRecipe.error = getString(R.string.error_insert_title)
            return false
        }

        //test categories
        if (layoutChipCategoryCreateRecipe.size == 0) {
            val errorCategoriesFragment =
                DialogErrorNoCategoryFragment()
            val fm = supportFragmentManager
            errorCategoriesFragment.show(fm, "no category error fragment")
            return false
        }

        return true
    }

    //fill layout filds with info in video
    private fun fillFields(
        title: String,
        description: String,
        videoId: String? = null,
        isFavorite: Boolean?
    ) {

        etRecipeNameCreateRecipe.setText(title)
        tvVideoDescription.setText(description)
        etYoutubeLinkCreateRecipe.setText(videoId)
        if (isFavorite != null) {
            checkBoxAddFavorite.isChecked = isFavorite
        }

        //show fields on ViewFlipper
        viewFlipperCreateRecipeActivity.displayedChild = 1

    }

    companion object {
        fun getStartIntent(context: Context, recipe: Recipe): Intent {
            return Intent(context, CreateRecipeActivity::class.java).apply {
                putExtra(AppConstantCodes.EXTRA_RECIPE, recipe)
            }
        }
    }
}