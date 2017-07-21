package com.e2esp.fcmagazine.adapters;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.interfaces.OnMagazineClickListener;
import com.e2esp.fcmagazine.models.Magazines;
import com.e2esp.fcmagazine.models.Magazines;

import java.util.ArrayList;

/**
 * Created by Zain on 2/10/2017.
 */

public class MagazineRecyclerAdapters extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_ITEM_LATEST_ISSUE = 1;
    private static final int VIEW_TYPE_HEADER_LATEST = 2;
    private static final int VIEW_TYPE_HEADER_RECENT = 3;
    private static final int VIEW_TYPE_HEADER_DOWNLOADED = 4;

    private Context context;
    private LayoutInflater layoutInflater;
    private ArrayList<Magazines> magazinesListLatest;
    private ArrayList<Magazines> magazinesListRecent;
    private ArrayList<Magazines> magazinesListDownloaded;
    private OnMagazineClickListener onMagazineClickListeners;

    public MagazineRecyclerAdapters(Context context, ArrayList<Magazines> magazinesListLatest, ArrayList<Magazines> magazinesListRecent, ArrayList<Magazines> magazinesListDownloaded, OnMagazineClickListener onMagazineClickListeners) {
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.magazinesListLatest = magazinesListLatest;
        this.magazinesListRecent = magazinesListRecent;
        this.magazinesListDownloaded = magazinesListDownloaded;
        this.onMagazineClickListeners = onMagazineClickListeners;
    }

    @Override
    public int getItemCount() {
        int itemCount = 0;
        if (magazinesListLatest.size() > 0) {
            itemCount += magazinesListLatest.size() + 1;
        }
        if (magazinesListRecent.size() > 0) {
            itemCount += magazinesListRecent.size() + 1;
        }
        if (magazinesListDownloaded.size() > 0) {
            itemCount += magazinesListDownloaded.size() + 1;
        }
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        int headers = 0;
        int items = 0;
        if (magazinesListLatest.size() > 0) {
            if (position == items + headers) {
                return VIEW_TYPE_HEADER_LATEST;
            }
            headers++;
            if (position < magazinesListLatest.size() + items + headers) {
                return VIEW_TYPE_ITEM_LATEST_ISSUE;
            }
            items += magazinesListLatest.size();
        }
        if (magazinesListRecent.size() > 0) {
            if (position == items + headers) {
                return VIEW_TYPE_HEADER_RECENT;
            }
            headers++;
            if (position < magazinesListRecent.size() + items + headers) {
                return VIEW_TYPE_ITEM;
            }
            items += magazinesListRecent.size();
        }
        if (magazinesListDownloaded.size() > 0) {
            if (position == items + headers) {
                return VIEW_TYPE_HEADER_DOWNLOADED;
            }
            headers++;
            if (position < magazinesListDownloaded.size() + items + headers) {
                return VIEW_TYPE_ITEM;
            }
            items += magazinesListDownloaded.size();
        }
        return VIEW_TYPE_ITEM;
    }

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize ( int position){
                int viewType = getItemViewType(position);
                if (viewType == VIEW_TYPE_ITEM) {
                    return 1;
                }
                return 2;
            }
        };
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = layoutInflater.inflate(R.layout.magazine_item_layout, parent, false);
            return new VHItem(view);
        }
        if (viewType == VIEW_TYPE_ITEM_LATEST_ISSUE) {
            View view = layoutInflater.inflate(R.layout.magazine_latest_issue_layout, parent, false);
            return new VHItem(view);
        }
        View view = layoutInflater.inflate(R.layout.magazine_header_layout, parent, false);
        if (viewType == VIEW_TYPE_HEADER_LATEST) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            view.setLayoutParams(params);
        }
        return new VHHeader(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int headers = 0;
        int items = 0;
        if (magazinesListLatest.size() > 0) {
            if (position == items + headers) {
                ((VHHeader) holder).bindView(context.getString(R.string.latest_issue));
                return;
            }
            headers++;
            if (position < magazinesListLatest.size() + items + headers) {
                ((VHItem) holder).bindView(magazinesListLatest.get(position - items - headers));
                return;
            }
            items += magazinesListLatest.size();
        }
        if (magazinesListRecent.size() > 0) {
            if (position == items + headers) {
                ((VHHeader) holder).bindView(context.getString(R.string.recent_issues));
                return;
            }
            headers++;
            if (position < magazinesListRecent.size() + items + headers) {
                ((VHItem) holder).bindView(magazinesListRecent.get(position - items - headers));
                return;
            }
            items += magazinesListRecent.size();
        }
        if (magazinesListDownloaded.size() > 0) {
            if (position == items + headers) {
                ((VHHeader) holder).bindView(context.getString(R.string.downloaded));
                return;
            }
            headers++;
            if (position < magazinesListDownloaded.size() + items + headers) {
                ((VHItem) holder).bindView(magazinesListDownloaded.get(position - items - headers));
                return;
            }
            items += magazinesListDownloaded.size();
        }
    }

    public class VHItem extends RecyclerView.ViewHolder {

        private View topView;
        private ImageView imageViewCover;
        private TextView textViewName;

        public VHItem(View itemView) {
            super(itemView);
            topView = itemView;
            imageViewCover = (ImageView) itemView.findViewById(R.id.imageViewCover);
            textViewName = (TextView) itemView.findViewById(R.id.textViewName);
        }

        public void bindView(final Magazines magazine) {
            if (magazine.isSpaceFiller()) {
                imageViewCover.setVisibility(View.INVISIBLE);
                textViewName.setVisibility(View.INVISIBLE);
            } else {
                //int image = Integer.parseInt(magazine.getFilePath());
                imageViewCover.setImageBitmap(magazine.getCover());
                textViewName.setText(magazine.getName());
                topView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onMagazineClickListeners.onMagazineClicked(magazine);
                    }
                });
            }
        }

    }

    public class VHHeader extends RecyclerView.ViewHolder {
        private TextView textViewHeader;
        public VHHeader(View headerView) {
            super(headerView);
            textViewHeader = (TextView) headerView.findViewById(R.id.textViewHeader);
        }
        public void bindView(String heading) {
            textViewHeader.setText(heading);
        }
    }

}






/*
package com.e2esp.fcmagazine.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.e2esp.fcmagazine.R;
import com.e2esp.fcmagazine.interfaces.OnMagazineClickListener;
import com.e2esp.fcmagazine.models.Magazines;

import java.util.ArrayList;

*/
/**
 * Created by Ali on 7/21/2017.
 *//*


public class MagazineRecyclerAdapters extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private ArrayList<Magazines> magazines;
    private OnMagazineClickListener onMagazineClickListener;
    private LayoutInflater layoutInflater;

    public MagazineRecyclerAdapters(Context context, ArrayList<Magazines> magazines, OnMagazineClickListener onMagazineClickListener, LayoutInflater layoutInflater) {
        this.context = context;
        this.magazines = magazines;
        this.onMagazineClickListener = onMagazineClickListener;
        this.layoutInflater = layoutInflater;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.show_cover_pages, parent, false);
        return new MagazinesViewHolder(view);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {

        return magazines.size();
    }

    public class MagazinesViewHolder extends RecyclerView.ViewHolder {

        private View topView;
        private ImageView imageViewCover;
        private TextView magazinesName;

        public MagazinesViewHolder(View itemView) {
            super(itemView);
            topView = itemView;
            imageViewCover = (ImageView) itemView.findViewById(R.id.imageViewMagazine);
            magazinesName = (TextView) itemView.findViewById(R.id.magazineTitle);
        }
    }

}
*/
