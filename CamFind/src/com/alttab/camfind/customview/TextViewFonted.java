package com.alttab.camfind.customview;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.alttab.camfind.R;

/**
 * Applies a pressed state color filter or disabled state alpha for the button's
 * background drawable.
 * 
 * @author shiki
 */
public class TextViewFonted extends TextView {

	public TextViewFonted(Context context, AttributeSet attrs) {
		super(context, attrs);
		UiUtil.setCustomFont(this, context, attrs,
				R.styleable.fontTypefaceButton,
				R.styleable.fontTypefaceButton_font);
	}
	public TextViewFonted(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		UiUtil.setCustomFont(this, context, attrs,
				R.styleable.fontTypefaceButton,
				R.styleable.fontTypefaceButton_font);
	}
}
