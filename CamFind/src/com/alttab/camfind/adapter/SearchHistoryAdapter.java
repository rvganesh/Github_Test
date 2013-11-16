package com.alttab.camfind.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.alttab.camfind.R;
import com.alttab.camfind.bean.SearchBean;
import com.alttab.camfind.listener.OnSwipeTouchListener;
import com.alttab.camfind.util.LazyLoad;
import com.common.MathUtils;
import com.nineoldandroids.animation.ObjectAnimator;
public class SearchHistoryAdapter extends BaseAdapter 
{
	Handler handler;
	private ArrayList<SearchBean> searchBeanArrayList;
	private Context context ;
	private LayoutInflater layoutInflater;
	
	private LazyLoad lazyLoad;
	private int optionLayoutWidth;
	public SearchHistoryAdapter(Context context,ArrayList<SearchBean> item)
	{
		this.context			=context;
		this.searchBeanArrayList=item;
		handler					= new Handler();
		lazyLoad 				=  new LazyLoad(context);
		layoutInflater 			= (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		optionLayoutWidth		= MathUtils.convertDpToPx(150, context);
		
	}

	@Override
	public int getCount() {
		return searchBeanArrayList.size();
	}

	@Override
	public Object getItem(int position) {
		return searchBeanArrayList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	RelativeLayout.LayoutParams params ;
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		final ViewHolder holder;

		if(convertView==null)
		{
			holder=new ViewHolder();
			convertView=layoutInflater.inflate(R.layout.search_history_listitem, null);
			holder.baseLayout =(RelativeLayout)convertView.findViewById(R.id.baseLayout);
			holder.listLayout =(RelativeLayout)convertView.findViewById(R.id.listLayout);
			holder.optionLayout= (LinearLayout)convertView.findViewById(R.id.optionLayout);
			holder.ivSearchPhoto=(ImageView) convertView.findViewById(R.id.imgPhoto);
			holder.txtName=(TextView) convertView.findViewById(R.id.txtName);
			holder.txtDesc=(TextView) convertView.findViewById(R.id.txtDesc);
			convertView.setTag(holder);
		}
		else
		{
			holder=(ViewHolder)convertView.getTag();
		}
		final SearchBean bean =searchBeanArrayList.get(position);
		
		
		holder.txtName.setText(bean.getSearchText());
		holder.txtDesc.setText(bean.getSearchDate());
		lazyLoad.LoadImagesFromURI(holder.ivSearchPhoto,bean.getSearchImageThumbnailPath());
		
		
		holder.baseLayout.setOnTouchListener(new OnSwipeTouchListener() 
		{

			@Override
			public void onMove(float difference)
			{
				if(!bean.getSwiped())
				{
					if(difference>0&&difference<optionLayoutWidth)
					{
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", difference).setDuration(0).start();
						params =new RelativeLayout.LayoutParams(Math.round(difference),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
				}
				else
				{
					if(difference<0&&difference>-optionLayoutWidth)
					{
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", optionLayoutWidth+difference).setDuration(0).start();
						params =new RelativeLayout.LayoutParams(Math.round(optionLayoutWidth+difference),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
				}
					
			}

			@Override
			public void onUp(float difference)
			{
				if(!bean.getSwiped())
				{
					if(difference>(optionLayoutWidth/2))
					{
						bean.setSwiped(true);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", optionLayoutWidth).start();
						params =new RelativeLayout.LayoutParams(Math.round(optionLayoutWidth),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
					else
					{
						bean.setSwiped(false);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", 0).start();
						params =new RelativeLayout.LayoutParams(Math.round(0),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
				}
				else
				{
					if(difference>-(optionLayoutWidth/2))
					{
						bean.setSwiped(true);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", optionLayoutWidth).start();
						params =new RelativeLayout.LayoutParams(Math.round(optionLayoutWidth),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
					else
					{
						bean.setSwiped(false);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", 0).start();
						params =new RelativeLayout.LayoutParams(Math.round(0),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}	
				}
			}

			@Override
			public void onCancel(float difference)
			{
				if(!bean.getSwiped())
				{
					if(difference>(optionLayoutWidth/2))
					{
						bean.setSwiped(true);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", optionLayoutWidth).start();
						params =new RelativeLayout.LayoutParams(Math.round(optionLayoutWidth),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
					else
					{
						bean.setSwiped(false);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", 0).start();
						params =new RelativeLayout.LayoutParams(Math.round(0),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
				}
				else
				{
					if(difference>-(optionLayoutWidth/2))
					{
						bean.setSwiped(true);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", optionLayoutWidth).start();
						params =new RelativeLayout.LayoutParams(Math.round(optionLayoutWidth),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}
					else
					{
						bean.setSwiped(false);
						ObjectAnimator.ofFloat(holder.listLayout, "translationX", 0).start();
						params =new RelativeLayout.LayoutParams(Math.round(0),LayoutParams.MATCH_PARENT);
						params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
						holder.optionLayout.setLayoutParams(params);
					}	
				}
			}

			@Override
			public void onSwipeRight() 
			{
			}

			@Override
			public void onSwipeLeft() 
			{
			}

		});

		return convertView;
	}

	class ViewHolder 
	{
		ImageView ivSearchPhoto;
		TextView txtName;
		TextView txtDesc;
		LinearLayout optionLayout;
		RelativeLayout baseLayout,listLayout;
	}

}
